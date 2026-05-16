package queue

import (
	"sync"
	"time"

	"github.com/google/uuid"
)

// ── Types ─────────────────────────────────────────────────────────────────────

type Status string

const (
	StatusQueued  Status = "queued"
	StatusRunning Status = "running"
	StatusDone    Status = "done"
	StatusError   Status = "error"
)

type Result struct {
	FasmOutput string `json:"fasm_output"`
	RunOutput  string `json:"run_output"`
	ExitCode   int    `json:"exit_code"`
}

type Job struct {
	ID        string    `json:"id"`
	Status    Status    `json:"status"`
	Position  int       `json:"position,omitempty"`
	CreatedAt time.Time `json:"created_at"`
	Result    *Result   `json:"result,omitempty"`
	Error     string    `json:"error,omitempty"`
}

// ── Queue ─────────────────────────────────────────────────────────────────────

const (
	MaxWorkers   = 3
	JobTTL       = 5 * time.Minute
	QueueTimeout = 30 * time.Second
)

type Queue struct {
	mu      sync.Mutex
	jobs    map[string]*Job
	order   []*Job
	workers chan struct{}
	worker  WorkerFunc
}

// WorkerFunc is the function that does the actual work for a job.
// Swap fake implementation for real fasm execution later.
type WorkerFunc func(job *Job)

func New(worker WorkerFunc) *Queue {
	q := &Queue{
		jobs:    make(map[string]*Job),
		workers: make(chan struct{}, MaxWorkers),
		worker:  worker,
	}
	q.startCleaner()
	return q
}

// Submit creates a new job, adds it to the queue and returns it.
func (q *Queue) Submit() *Job {
	job := &Job{
		ID:        uuid.New().String(),
		Status:    StatusQueued,
		CreatedAt: time.Now(),
	}

	q.mu.Lock()
	q.jobs[job.ID] = job
	q.order = append(q.order, job)
	job.Position = q.position(job.ID)
	q.mu.Unlock()

	go q.dispatch(job)

	return job
}

// Get returns a job by ID with updated queue position.
func (q *Queue) Get(id string) (*Job, bool) {
	q.mu.Lock()
	defer q.mu.Unlock()

	job, ok := q.jobs[id]
	if ok && job.Status == StatusQueued {
		job.Position = q.position(id)
	}
	return job, ok
}

// dispatch waits for a free worker slot then runs the job.
func (q *Queue) dispatch(job *Job) {
	select {
	case q.workers <- struct{}{}:
		defer func() { <-q.workers }()

		q.mu.Lock()
		job.Status = StatusRunning
		job.Position = 0
		q.mu.Unlock()

		q.worker(job)

	case <-time.After(QueueTimeout):
		q.mu.Lock()
		job.Status = StatusError
		job.Error = "server busy, try again later"
		q.mu.Unlock()
	}
}

// position calculates queue position — call with lock held.
func (q *Queue) position(id string) int {
	pos := 0
	for _, j := range q.order {
		if j.Status == StatusQueued {
			pos++
		}
		if j.ID == id {
			break
		}
	}
	return pos
}

// startCleaner runs a background goroutine that removes old jobs.
func (q *Queue) startCleaner() {
	go func() {
		for {
			time.Sleep(1 * time.Minute)
			q.mu.Lock()

			for id, j := range q.jobs {
				if time.Since(j.CreatedAt) > JobTTL {
					delete(q.jobs, id)
				}
			}

			active := q.order[:0]
			for _, j := range q.order {
				if j.Status == StatusQueued || j.Status == StatusRunning {
					active = append(active, j)
				}
			}
			q.order = active

			q.mu.Unlock()
		}
	}()
}
