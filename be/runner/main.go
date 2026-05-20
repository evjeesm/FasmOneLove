package main

import (
	"fmt"
	"os"
	"syscall"
	"unsafe"

	"golang.org/x/sys/unix"
)

const (
	rlimitMemBytes  = 64 * 1024 * 1024 // 64MB virtual memory
	rlimitFileBytes = 512 * 1024        // 512KB max file write
	rlimitCPUSecs   = 5                 // 5 seconds CPU time
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "usage: runner <binary>")
		os.Exit(1)
	}

	binaryPath := os.Args[1]

	if err := applyRlimits(); err != nil {
		fmt.Fprintf(os.Stderr, "rlimits failed: %v\n", err)
		os.Exit(1)
	}

	if err := applySeccomp(); err != nil {
		fmt.Fprintf(os.Stderr, "seccomp failed: %v\n", err)
		os.Exit(1)
	}

	if err := syscall.Exec(binaryPath, []string{binaryPath}, []string{}); err != nil {
		fmt.Fprintf(os.Stderr, "exec failed: %v\n", err)
		os.Exit(1)
	}
}

// ── Rlimits ───────────────────────────────────────────────────────────────────

func applyRlimits() error {
	limits := []struct {
		resource int
		limit    unix.Rlimit
	}{
		{unix.RLIMIT_AS,    unix.Rlimit{Cur: rlimitMemBytes, Max: rlimitMemBytes}},
		{unix.RLIMIT_FSIZE, unix.Rlimit{Cur: rlimitFileBytes, Max: rlimitFileBytes}},
		{unix.RLIMIT_CPU,   unix.Rlimit{Cur: rlimitCPUSecs, Max: rlimitCPUSecs}},
		{unix.RLIMIT_NPROC, unix.Rlimit{Cur: 0, Max: 0}},
	}
	for _, l := range limits {
		if err := unix.Setrlimit(l.resource, &l.limit); err != nil {
			return err
		}
	}
	return nil
}

// ── Seccomp ───────────────────────────────────────────────────────────────────

func applySeccomp() error {
	blocked := []uint32{
		// Network
		41, 42, 49, 50, 43, 288, 44, 45,
		// socket, connect, bind, listen, accept, accept4, sendto, recvfrom
		// Process spawning
		57, 58, 56, /*59, //allow execve */ 322,
		// fork, vfork, clone, execve, execveat
		// Filesystem manipulation
		165, 166, 155, 161,
		// mount, umount2, pivot_root, chroot
		// Kernel manipulation
		175, 313, 246, 169, 164,
		// init_module, finit_module, kexec_load, reboot, settimeofday
		// Spying
		101, // ptrace
	}

	var filter []unix.SockFilter

	// Validate architecture == x86_64
	filter = append(filter,
		bpfStmt(unix.BPF_LD|unix.BPF_W|unix.BPF_ABS, 4),
		bpfJump(unix.BPF_JMP|unix.BPF_JEQ|unix.BPF_K, 0xc000003e, 1, 0),
		bpfStmt(unix.BPF_RET|unix.BPF_K, unix.SECCOMP_RET_KILL),
	)

	// Load syscall number
	filter = append(filter,
		bpfStmt(unix.BPF_LD|unix.BPF_W|unix.BPF_ABS, 0),
	)

	// Kill on each blocked syscall
	for _, nr := range blocked {
		filter = append(filter,
			bpfJump(unix.BPF_JMP|unix.BPF_JEQ|unix.BPF_K, nr, 0, 1),
			bpfStmt(unix.BPF_RET|unix.BPF_K, unix.SECCOMP_RET_KILL),
		)
	}

	// Default: allow
	filter = append(filter,
		bpfStmt(unix.BPF_RET|unix.BPF_K, unix.SECCOMP_RET_ALLOW),
	)

	prog := unix.SockFprog{
		Len:    uint16(len(filter)),
		Filter: &filter[0],
	}

	if err := unix.Prctl(unix.PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0); err != nil {
		return err
	}

	_, _, errno := unix.Syscall(
		unix.SYS_SECCOMP,
		unix.SECCOMP_SET_MODE_FILTER,
		0,
		uintptr(unsafe.Pointer(&prog)),
	)
	if errno != 0 {
		return errno
	}
	return nil
}

func bpfStmt(code uint16, k uint32) unix.SockFilter {
	return unix.SockFilter{Code: code, K: k}
}

func bpfJump(code uint16, k uint32, jt, jf uint8) unix.SockFilter {
	return unix.SockFilter{Code: code, Jt: jt, Jf: jf, K: k}
}
