package main

import (
	"fmt"
	"net/http"
	"os"

	"fasmonelove/handler"
	"fasmonelove/queue"
	"fasmonelove/sandbox"
)

func main() {
	q := queue.New(sandbox.Run)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintln(w, `{"status":"ok"}`)
	})
	http.HandleFunc("/compile", handler.Compile(q))
	http.HandleFunc("/job/", handler.Status(q))

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	fmt.Printf("listening on :%s\n", port)
	http.ListenAndServe(":"+port, nil)
}
