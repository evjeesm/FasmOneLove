package handler

import (
	"encoding/json"
	"io"
	"net/http"
	"os"

	"fasmonelove/packer"
	"fasmonelove/queue"

	"github.com/google/uuid"
)

const maxUploadBytes = 1 << 20 // 1MB

func Compile(q *queue.Queue) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		// Parse multipart form (max 1MB)
		if err := r.ParseMultipartForm(maxUploadBytes); err != nil {
			http.Error(w, "request too large or malformed", http.StatusBadRequest)
			return
		}

		f, _, err := r.FormFile("archive")
		if err != nil {
			http.Error(w, "missing 'archive' field", http.StatusBadRequest)
			return
		}
		defer f.Close()

		// Read archive bytes (limit to 1MB)
		tarBytes, err := io.ReadAll(io.LimitReader(f, maxUploadBytes+1))
		if err != nil {
			http.Error(w, "failed to read archive", http.StatusInternalServerError)
			return
		}
		if len(tarBytes) > maxUploadBytes {
			http.Error(w, "archive too large (max 1MB)", http.StatusRequestEntityTooLarge)
			return
		}

		// Create work dir for this job
		workDir := "/tmp/" + uuid.New().String()
		if err := os.MkdirAll(workDir, 0755); err != nil {
			http.Error(w, "failed to create work dir", http.StatusInternalServerError)
			return
		}

		// Extract archive into work dir
		if err := packer.Extract(tarBytes, workDir); err != nil {
			os.RemoveAll(workDir)
			http.Error(w, "invalid archive: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Submit job
		entrypoint := r.FormValue("entrypoint")
		if entrypoint == "" {
			entrypoint = "main.asm"
		}

		// Fasm Options
		opts := r.FormValue("fasmOptions")

		job := q.Submit(workDir, entrypoint)

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusAccepted)
		json.NewEncoder(w).Encode(job)
	}
}
