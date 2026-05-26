package jekas.obps.fasmonelove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.model.CompileSettings
import jekas.obps.fasmonelove.model.WorkspaceManager
import jekas.obps.fasmonelove.ui.theme.*
import java.io.File

class WorkspaceSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workspaceName = intent.getStringExtra("workspace_name") ?: ""
        val workspacePath = intent.getStringExtra("workspace_path") ?: ""

        setContent {
            FasmOneLoveTheme {
                WorkspaceSettingsScreen(
                    workspaceName = workspaceName,
                    workspacePath = workspacePath,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSettingsScreen(
    workspaceName: String,
    workspacePath: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val workspace = WorkspaceManager.listWorkspaces(context)
        .firstOrNull { it.name == workspaceName }

    var entrypoint  by remember { mutableStateOf(workspace?.settings?.entrypoint ?: "main.asm") }
    var output      by remember { mutableStateOf(workspace?.settings?.output ?: "") }
    var memory      by remember { mutableStateOf(workspace?.settings?.memory?.takeIf { it > 0 }?.toString() ?: "") }
    var maxPasses   by remember { mutableStateOf(workspace?.settings?.maxPasses?.takeIf { it > 0 }?.toString() ?: "") }
    var run         by remember { mutableStateOf(workspace?.settings?.run ?: false) }
    var defines     by remember { mutableStateOf(
        workspace?.settings?.defines?.entries?.joinToString("\n") { "${it.key}=${it.value}" } ?: ""
    )}

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(workspaceName, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // Parse defines back to map
                        val definesMap = defines.lines()
                            .filter { it.isNotBlank() }
                            .associate { line ->
                                val parts = line.split("=", limit = 2)
                                parts[0].trim() to (parts.getOrNull(1)?.trim() ?: "")
                            }

                        WorkspaceManager.saveSettings(
                            dir = File(workspacePath),
                            settings = CompileSettings(
                                entrypoint = entrypoint.trim().ifEmpty { "main.asm" },
                                output = output.trim(),
                                memory = memory.toIntOrNull() ?: 0,
                                maxPasses = maxPasses.toIntOrNull() ?: 0,
                                run = run,
                                defines = definesMap,
                            )
                        )
                        onBack()
                    }) {
                        Text("Save", color = LinkBlue)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Entrypoint ────────────────────────────────────────────────────
            SettingsTextField(
                label = "Entrypoint",
                value = entrypoint,
                onValueChange = { entrypoint = it },
                placeholder = "main.asm",
                hint = "Source file passed to fasm",
            )

            // ── Output ────────────────────────────────────────────────────────
            SettingsTextField(
                label = "Output file",
                value = output,
                onValueChange = { output = it },
                placeholder = "leave empty — let fasm decide",
                hint = "Output filename, relative to workspace",
            )

            // ── Memory ────────────────────────────────────────────────────────
            SettingsTextField(
                label = "Memory (KB)",
                value = memory,
                onValueChange = { memory = it },
                placeholder = "default",
                hint = "fasm -m flag, 0 = default",
                keyboardType = KeyboardType.Number,
            )

            // ── Max passes ────────────────────────────────────────────────────
            SettingsTextField(
                label = "Max passes",
                value = maxPasses,
                onValueChange = { maxPasses = it },
                placeholder = "default",
                hint = "fasm -p flag, 0 = default",
                keyboardType = KeyboardType.Number,
            )

            // ── Defines ───────────────────────────────────────────────────────
            SettingsTextField(
                label = "Defines",
                value = defines,
                onValueChange = { defines = it },
                placeholder = "DEBUG=1\nVERSION=2",
                hint = "One per line, name=value or just name",
                singleLine = false,
                minLines = 3,
            )

            // ── Run after compile ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Run after compile", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Execute binary in sandbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = run,
                    onCheckedChange = { run = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LinkBlue,
                        checkedTrackColor = BorderNavy,
                    )
                )
            }
        }
    }
}

// ── Reusable settings text field ──────────────────────────────────────────────

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    hint: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = TextMuted) },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LinkBlue,
                unfocusedBorderColor = BorderNavy,
                focusedLabelColor = LinkBlue,
                unfocusedLabelColor = TextMuted,
                cursorColor = LinkBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
            )
        )
        if (hint.isNotEmpty()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}