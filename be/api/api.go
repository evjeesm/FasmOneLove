package api

// CompileRequest holds all options for a compile job.
// Sent as JSON in the 'options' multipart field.
type CompileRequest struct {
	// Entrypoint is the source file to compile (required)
	Entrypoint string `json:"entrypoint"`

	// Output is the output file path relative to WorkDir.
	// Empty string lets fasm decide (strips .asm extension by default).
	Output string `json:"output,omitempty"`

	// Defines maps -d name=value flags. Empty value means -d name only.
	Defines map[string]string `json:"defines,omitempty"`

	// Memory sets fasm's -m <kbytes> memory limit. 0 = fasm default.
	Memory int `json:"memory,omitempty"`

	// MaxPasses sets fasm's -p <passes> limit. 0 = fasm default.
	MaxPasses int `json:"max_passes,omitempty"`

	// FasmEnv sets extra environment variables for the fasm process.
	FasmEnv map[string]string `json:"fasm_env,omitempty"`

	// Run controls whether the compiled binary is executed in the jail.
	// Defaults to false — safe for object files, raw binaries, etc.
	Run bool `json:"run,omitempty"`

	// RunEnv sets environment variables for the binary execution.
	// Only used when Run is true.
	RunEnv map[string]string `json:"run_env,omitempty"`
}
