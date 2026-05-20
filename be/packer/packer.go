package packer

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// Extract safely unpacks a tar.gz byte slice into destDir.
// Rejects path traversal attempts.
func Extract(data []byte, destDir string) error {
	gr, err := gzip.NewReader(bytes.NewReader(data))
	if err != nil {
		return fmt.Errorf("invalid gzip: %w", err)
	}
	defer gr.Close()

	tr := tar.NewReader(gr)

	for {
		header, err := tr.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("tar read error: %w", err)
		}

		// Safety: reject absolute paths and traversal
		if filepath.IsAbs(header.Name) || strings.Contains(header.Name, "..") {
			return fmt.Errorf("unsafe path in archive: %s", header.Name)
		}

		target := filepath.Join(destDir, header.Name)

		switch header.Typeflag {
		case tar.TypeDir:
			if err := os.MkdirAll(target, 0755); err != nil {
				return fmt.Errorf("mkdir %s: %w", target, err)
			}

		case tar.TypeReg:
			// Ensure parent dir exists
			if err := os.MkdirAll(filepath.Dir(target), 0755); err != nil {
				return fmt.Errorf("mkdir parent %s: %w", target, err)
			}

			f, err := os.Create(target)
			if err != nil {
				return fmt.Errorf("create file %s: %w", target, err)
			}

			// Limit individual file size to 1MB
			if _, err := io.Copy(f, io.LimitReader(tr, 1<<20)); err != nil {
				f.Close()
				return fmt.Errorf("write file %s: %w", target, err)
			}
			f.Close()
		}
	}

	return nil
}

// Snapshot returns the set of all file paths currently in dir.
// Used to detect which files are new after fasm runs.
func Snapshot(dir string) (map[string]bool, error) {
	files := make(map[string]bool)

	err := filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() {
			files[path] = true
		}
		return nil
	})

	return files, err
}

// Diff returns paths that are in after but not in before.
// These are the files produced by fasm.
func Diff(before, after map[string]bool) []string {
	var newFiles []string
	for path := range after {
		if !before[path] {
			newFiles = append(newFiles, path)
		}
	}
	return newFiles
}

// Pack creates a tar.gz archive from the given file paths.
// Paths are stored relative to baseDir.
func Pack(paths []string, baseDir string, wrapDir string) ([]byte, error) {
	var buf bytes.Buffer

	gw := gzip.NewWriter(&buf)
	tw := tar.NewWriter(gw)

	for _, path := range paths {
		info, err := os.Stat(path)
		if err != nil {
			return nil, fmt.Errorf("stat %s: %w", path, err)
		}

		// Store relative path in archive
		rel, err := filepath.Rel(baseDir, path)
		if err != nil {
			return nil, fmt.Errorf("rel path %s: %w", path, err)
		}

		rel = filepath.Join(wrapDir, rel)

		header := &tar.Header{
			Name:    rel,
			Size:    info.Size(),
			Mode:    int64(info.Mode()),
			ModTime: info.ModTime(),
		}

		if err := tw.WriteHeader(header); err != nil {
			return nil, fmt.Errorf("write header %s: %w", rel, err)
		}

		f, err := os.Open(path)
		if err != nil {
			return nil, fmt.Errorf("open %s: %w", path, err)
		}

		if _, err := io.Copy(tw, f); err != nil {
			f.Close()
			return nil, fmt.Errorf("copy %s: %w", path, err)
		}
		f.Close()
	}

	if err := tw.Close(); err != nil {
		return nil, fmt.Errorf("close tar: %w", err)
	}
	if err := gw.Close(); err != nil {
		return nil, fmt.Errorf("close gzip: %w", err)
	}

	return buf.Bytes(), nil
}
