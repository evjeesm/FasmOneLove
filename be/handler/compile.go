package handler

import (
	"encoding/json"
	"io"
	"net/http"
	"os"

	"fasmonelove/api"
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

		if err := r.ParseMultipartForm(maxUploadBytes); err != nil {
			http.Error(w, "request too large or malformed", http.StatusBadRequest)
			return
		}

		// Parse options JSON
		var req api.CompileRequest
		optionsStr := r.FormValue("options")
		if optionsStr != "" {
			if err := json.Unmarshal([]byte(optionsStr), &req); err != nil {
				http.Error(w, "invalid options JSON: "+err.Error(), http.StatusBadRequest)
				return
			}
		}
		if req.Entrypoint == "" {
			req.Entrypoint = "main.asm"
		}

		// Read archive
		f, _, err := r.FormFile("archive")
		if err != nil {
			http.Error(w, "missing 'archive' field", http.StatusBadRequest)
			return
		}
		defer f.Close()

		tarBytes, err := io.ReadAll(io.LimitReader(f, maxUploadBytes+1))
		if err != nil {
			http.Error(w, "failed to read archive", http.StatusInternalServerError)
			return
		}
		if len(tarBytes) > maxUploadBytes {
			http.Error(w, "archive too large (max 1MB)", http.StatusRequestEntityTooLarge)
			return
		}

		// Create work dir and extract
		workDir := "/tmp/" + uuid.New().String()
		if err := os.MkdirAll(workDir, 0755); err != nil {
			http.Error(w, "failed to create work dir", http.StatusInternalServerError)
			return
		}

		if err := packer.Extract(tarBytes, workDir); err != nil {
			os.RemoveAll(workDir)
			http.Error(w, "invalid archive: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Submit job
		job := q.Submit(workDir, req)

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusAccepted)
		json.NewEncoder(w).Encode(job)
	}
}
