package sandbox

import (
	"math/rand"
	"time"

	"fasmonelove/queue"
)

// Run is the WorkerFunc passed to queue.New().
// Currently simulates fasm compile + run with fake output.
// Replace internals with real chroot + fasm + seccomp execution later.
func Run(job *queue.Job) {
	// Simulate fasm compilation (500ms - 1.5s)
	time.Sleep(time.Duration(500+rand.Intn(1000)) * time.Millisecond)

	// Simulate binary execution (200ms - 800ms)
	time.Sleep(time.Duration(200+rand.Intn(600)) * time.Millisecond)

	job.Status = queue.StatusDone
	job.Result = &queue.Result{
		FasmOutput: "flat assembler  version 1.73.32  (16384 kilobytes)\n1 passes, 512 bytes.",
		RunOutput:  "Hello from FasmOneLove!\n",
		ExitCode:   0,
	}
}
