package sandbox

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"time"

	"fasmonelove/api"
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

// Run is the real WorkerFunc — compiles with fasm,
// optionally runs the output binary with chroot + seccomp + resource limits.
func Run(job *queue.Job) {
	req := job.Request

	// Snapshot before compile
	before, err := packer.Snapshot(job.WorkDir)
	if err != nil {
		fail(job, "snapshot failed: "+err.Error())
		return
	}

	// Compile with fasm
	srcPath := filepath.Join(job.WorkDir, req.Entrypoint)
	args := buildFasmArgs(req, srcPath, job.WorkDir)
	env := buildEnv(req.FasmEnv)

	fasmOut, fasmErr, fasmExit := runFasm(args, env)
	if fasmExit != 0 {
		job.Status = queue.StatusDone
		job.Result = &queue.Result{
			FasmOutput: fasmOut + fasmErr,
			ExitCode:   fasmExit,
		}
		return
	}

	// Snapshot after compile
	after, err := packer.Snapshot(job.WorkDir)
	if err != nil {
		fail(job, "post-snapshot failed: "+err.Error())
		return
	}
	newFiles := packer.Diff(before, after)

	// Optionally run binary in chroot + seccomp sandbox
	var runOut, runErr string
	var runExit int
	if req.Run {
		outPath := resolveOutput(req, srcPath, job.WorkDir)
		runOut, runErr, runExit = runSandboxed(outPath, buildEnv(req.RunEnv))
	}

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

// buildFasmArgs constructs fasm command line from CompileRequest.
func buildFasmArgs(req api.CompileRequest, srcPath, workDir string) []string {
	var args []string

	if req.Memory > 0 {
		args = append(args, "-m", fmt.Sprintf("%d", req.Memory))
	}
	if req.MaxPasses > 0 {
		args = append(args, "-p", fmt.Sprintf("%d", req.MaxPasses))
	}
	for name, value := range req.Defines {
		if value == "" {
			args = append(args, "-d", name)
		} else {
			args = append(args, "-d", fmt.Sprintf("%s=%s", name, value))
		}
	}

	args = append(args, srcPath)

	if req.Output != "" {
		args = append(args, filepath.Join(workDir, req.Output))
	}

	return args
}

// buildEnv converts a map to KEY=VALUE slice.
func buildEnv(m map[string]string) []string {
	var env []string
	for k, v := range m {
		env = append(env, fmt.Sprintf("%s=%s", k, v))
	}
	return env
}

// resolveOutput finds the output binary path after compilation.
// If Output is specified use that, otherwise find the new file fasm created.
func resolveOutput(req api.CompileRequest, srcPath, workDir string) string {
	if req.Output != "" {
		return filepath.Join(workDir, req.Output)
	}
	// Fasm default: strip .asm extension
	base := srcPath
	if filepath.Ext(base) == ".asm" {
		base = base[:len(base)-4]
	}
	return base
}

// runFasm compiles using fasm binary with given args and env.
func runFasm(args, env []string) (stdout, stderr string, exitCode int) {
	cmd := exec.Command(fasmPath, args...)
	cmd.Env = env

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
func runSandboxed(binaryPath string, env []string) (stdout, stderr string, exitCode int) {
	jail, err := buildJail(binaryPath)
	if err != nil {
		return "", fmt.Sprintf("failed to build jail: %v", err), 1
	}
	defer os.RemoveAll(jail)

	// Run: chroot into jail, execute runner /binary
	cmd := exec.Command("/runner", "/binary")
	cmd.Env = env
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
// user binary + runner binary + essential shared libs.
// Uses /var/jail/ as base to avoid noexec on /tmp.
func buildJail(binaryPath string) (jail string, err error) {
	// Ensure base jail dir exists
	if err = os.MkdirAll("/tmp/jail", 0700); err != nil {
		return "", fmt.Errorf("create jail base: %w", err)
	}

	jail, err = os.MkdirTemp("/tmp/jail", "jail-*")
	if err != nil {
		return "", err
	}

	// Cleanup on any failure — cancelled on success

	if err = os.Chmod(jail, 0711); err != nil {
		return "", err
	}

	if err = os.MkdirAll(filepath.Join(jail, "tmp"), 0777); err != nil {
		return "", err
	}

	if err = copyFile(binaryPath, filepath.Join(jail, "binary"), 0111); err != nil {
		return "", fmt.Errorf("copy binary: %w", err)
	}

	if err = copyFile(runnerPath, filepath.Join(jail, "runner"), 0111); err != nil {
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
