package jekas.obps.fasmonelove.model

// ── Model ─────────────────────────────────────────────────────────────────────

data class CompileSettings(
    val entrypoint: String = "main.asm",
    val output: String = "",
    val defines: Map<String, String> = emptyMap(),
    val memory: Int = 0,
    val maxPasses: Int = 0,
    val run: Boolean = false,
    val runEnv: Map<String, String> = emptyMap()
)

data class Workspace(
    val name: String,
    val path: String,
    val settings: CompileSettings = CompileSettings()
)