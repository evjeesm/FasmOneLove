package sandbox

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"time"

	"fasmonelove/packer"
	"fasmonelove/queue"
)

const (
	fasmPath       = "/usr/bin/fasm"
	runnerPath     = "/runner" // built and installed by Dockerfile
	compileTimeout = 10 * time.Second
	runTimeout     = 5 * time.Second
	maxMemBytes    = 64 * 1024 * 1024 // 64MB
	maxFileBytes   = 512 * 1024       // 512KB
)

type FasmOptions struct {
	Output    *string           `json:"output,omitempty"`
	Defines   map[string]string `json:"defines,omitempty"`
	Memory    *int              `json:"memory,omitempty"`
	MaxPasses *int              `json:"maxPasses,omitempty"`
	Env       map[string]string `json:"env,omitempty"`
}

// Run is the real WorkerFunc — compiles with fasm,
// runs the output binary with chroot + seccomp + resource limits.
func Run(job *queue.Job) {
	// Make a snapshot of the workDir before run
	before, err := packer.Snapshot(job.WorkDir)
	if err != nil {
		fail(job, "snapshot failed: "+err.Error())
		return
	}

	// Execute job
	srcPath := filepath.Join(job.WorkDir, job.Entrypoint)
	outPath := filepath.Join(job.WorkDir, "output")

	fasmOut, fasmErr, fasmExit := runFasm(srcPath, outPath)
	if fasmExit != 0 {
		job.Status = queue.StatusDone
		job.Result = &queue.Result{
			FasmOutput: fasmOut + fasmErr,
			ExitCode:   fasmExit,
		}
		return
	}

	// Snapshot after fasm job is done
	after, err := packer.Snapshot(job.WorkDir)
	if err != nil {
		fail(job, "post-snapshot failed: "+err.Error())
		return
	}
	newFiles := packer.Diff(before, after)

	// Sandbox run resulting binary produced by FASM
	// TODO: Make this configurable by the user request
	runOut, runErr, runExit := runSandboxed(outPath)

	// Pack output files
	var outputArchive []byte
	if len(newFiles) > 0 {
		outputArchive, err = packer.Pack(newFiles, job.WorkDir, "")
		if err != nil {
			fail(job, "pack failed: "+err.Error())
			return
		}
	}

	job.Status = queue.StatusDone
	job.Result = &queue.Result{
		FasmOutput:    fasmOut + fasmErr,
		RunOutput:     runOut,
		RunStderr:     runErr,
		ExitCode:      runExit,
		OutputArchive: outputArchive,
	}
}

// runFasm compiles src -> out using fasm binary.
func runFasm(src, out string) (stdout, stderr string, exitCode int) {
	cmd := exec.Command(fasmPath, src, out)

	var outBuf, errBuf bytes.Buffer
	cmd.Stdout = &outBuf
	cmd.Stderr = &errBuf

	if err := cmd.Start(); err != nil {
		return "", fmt.Sprintf("failed to start fasm: %v", err), 1
	}

	done := make(chan error, 1)
	go func() { done <- cmd.Wait() }()

	select {
	case err := <-done:
		if err != nil {
			if exit, ok := err.(*exec.ExitError); ok {
				return outBuf.String(), errBuf.String(), exit.ExitCode()
			}
			return outBuf.String(), err.Error(), 1
		}
		return outBuf.String(), errBuf.String(), 0
	case <-time.After(compileTimeout):
		cmd.Process.Kill()
		return "", "compilation timed out", 1
	}
}

// runSandboxed runs the compiled binary inside a chroot jail.
// The jail contains: the user binary + runner binary + essential libs.
// runner applies rlimits + seccomp then execs the user binary.
func runSandboxed(binaryPath string) (stdout, stderr string, exitCode int) {
	jail, err := buildJail(binaryPath)
	defer os.RemoveAll(jail)
	if err != nil {
		return "", fmt.Sprintf("failed to build jail: %v", err), 1
	}

	// Run: chroot into jail, execute runner /binary
	cmd := exec.Command("/runner", "/binary")
	cmd.SysProcAttr = &syscall.SysProcAttr{
		Chroot: jail,
		Credential: &syscall.Credential{
			Uid: 65534, // nobody
			Gid: 65534,
		},
	}

	var outBuf, errBuf bytes.Buffer
	cmd.Stdout = &outBuf
	cmd.Stderr = &errBuf

	if err := cmd.Start(); err != nil {
		return "", fmt.Sprintf("failed to start sandbox: %v", err), 1
	}

	done := make(chan error, 1)
	go func() { done <- cmd.Wait() }()

	select {
	case err := <-done:
		out := outBuf.String()
		errStr := errBuf.String()
		if err != nil {
			if exit, ok := err.(*exec.ExitError); ok {
				return out, errStr, exit.ExitCode()
			}
			return out, err.Error(), 1
		}
		return out, errStr, 0
	case <-time.After(runTimeout):
		cmd.Process.Kill()
		return outBuf.String(), "process killed: wall-clock timeout exceeded", 1
	}
}

// buildJail creates a minimal chroot directory containing:
// user binary + runner binary.
// func do not cleans up itself on fail
// NOTE: optional: copy essential libraries from lib and lib64
func buildJail(binaryPath string) (string, error) {
	jail, err := os.MkdirTemp("", "jail-*")
	if err != nil {
		return "", err
	}

	err = os.Chmod(jail, 0711)
	if err != nil {
		return "", err
	}

	// For temporary file creation
	if err = os.MkdirAll(filepath.Join(jail, "tmp"), 0777); err != nil {
		return "", err
	}

	// Copy user binary into jail
	if err := copyFile(binaryPath, filepath.Join(jail, "binary"), 0111); err != nil {
		return "", fmt.Errorf("copy binary: %w", err)
	}

	// Copy runner binary into jail
	if err := copyFile(runnerPath, filepath.Join(jail, "runner"), 0111); err != nil {
		return "", fmt.Errorf("copy runner: %w", err)
	}

	return jail, nil
}

func copyFile(src, dst string, perm os.FileMode) error {
	if err := os.MkdirAll(filepath.Dir(dst), 0755); err != nil {
		return err
	}
	data, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	return os.WriteFile(dst, data, perm)
}

func fail(job *queue.Job, msg string) {
	job.Status = queue.StatusError
	job.Error = msg
}
