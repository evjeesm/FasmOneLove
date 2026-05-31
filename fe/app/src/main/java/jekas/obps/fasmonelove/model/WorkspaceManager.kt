package jekas.obps.fasmonelove.model

import android.content.Context
import org.json.JSONObject
import java.io.File


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

object WorkspaceManager {

    private fun workspacesRoot(context: Context): File {
        val root = File(context.getExternalFilesDir(null), "workspaces")
        root.mkdirs()
        return root
    }

    private fun indexFile(context: Context) =
        File(context.getExternalFilesDir(null), "workspaces.json")

    fun listWorkspaces(context: Context): List<Workspace> {
        val root = workspacesRoot(context)
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.map { dir ->
                val settings = loadSettings(dir)
                Workspace(name = dir.name, path = dir.absolutePath, settings = settings)
            }
            ?: emptyList()
    }

    fun createWorkspace(context: Context, name: String): Workspace {
        val dir = File(workspacesRoot(context), name)
        dir.mkdirs()

        // Create default main.asm
        File(dir, "main.asm").writeText(
            "format ELF64 executable\nentry start\n\nsegment readable executable\nstart:\n    ; your code here\n"
        )

        val workspace = Workspace(name = name, path = dir.absolutePath)
        saveSettings(dir, workspace.settings)
        saveLastOpened(context, name)
        return workspace
    }

    fun getLastOpened(context: Context): String? {
        val f = indexFile(context)
        if (!f.exists()) return null
        return try {
            JSONObject(f.readText()).optString("last_opened").takeIf { it.isNotEmpty() }
        } catch (e: Exception) { null }
    }

    fun saveLastOpened(context: Context, name: String) {
        val f = indexFile(context)
        val json = if (f.exists()) {
            try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
        } else JSONObject()
        json.put("last_opened", name)
        f.writeText(json.toString())
    }

    private fun loadSettings(dir: File): CompileSettings {
        val f = File(dir, ".workspace.json")
        if (!f.exists()) return CompileSettings()
        return try {
            val json = JSONObject(f.readText())
            CompileSettings(
                entrypoint = json.optString("entrypoint", "main.asm"),
                output = json.optString("output", ""),
                run = json.optBoolean("run", false),
                memory = json.optInt("memory", 0),
                maxPasses = json.optInt("max_passes", 0),
            )
        } catch (e: Exception) { CompileSettings() }
    }

    fun saveSettings(dir: File, settings: CompileSettings) {
        val json = JSONObject().apply {
            put("entrypoint", settings.entrypoint)
            put("output", settings.output)
            put("run", settings.run)
            put("memory", settings.memory)
            put("max_passes", settings.maxPasses)
        }
        File(dir, ".workspace.json").writeText(json.toString())
    }
}
