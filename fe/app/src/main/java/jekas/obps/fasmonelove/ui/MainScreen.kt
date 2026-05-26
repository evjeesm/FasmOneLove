package jekas.obps.fasmonelove.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.model.Workspace
import jekas.obps.fasmonelove.model.WorkspaceManager
import jekas.obps.fasmonelove.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val AUTOSAVE_DELAY_MS = 2000L

// ── Job status ────────────────────────────────────────────────────────────────

sealed class JobStatus {
    object Idle : JobStatus()
    data class Queued(val position: Int) : JobStatus()
    object Running : JobStatus()
    object Done : JobStatus()
    data class Error(val message: String) : JobStatus()
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var isDirty by remember { mutableStateOf(false) }
    var jobStatus by remember { mutableStateOf<JobStatus>(JobStatus.Idle) }
    var outputExpanded by remember { mutableStateOf(false) }
    var outputText by remember { mutableStateOf("") }

    // ── Load last opened workspace on start ───────────────────────────────────
    LaunchedEffect(Unit) {
        val lastOpened = WorkspaceManager.getLastOpened(context)
        currentWorkspace = WorkspaceManager.listWorkspaces(context)
            .firstOrNull { it.name == lastOpened }

        // Auto-open entrypoint file if workspace exists
        currentWorkspace?.let { ws ->
            val entrypoint = File(ws.path, ws.settings.entrypoint)
            if (entrypoint.exists()) {
                currentFile = entrypoint
                editorContent = entrypoint.readText()
            }
        }
    }

    // ── Auto-save — triggers 2s after last content change ─────────────────────
    LaunchedEffect(editorContent) {
        if (!isDirty) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        currentFile?.writeText(editorContent)
        isDirty = false
    }

    // ── Open file helper ──────────────────────────────────────────────────────
    fun openFile(file: File) {
        // Flush current file immediately before switching
        if (isDirty) {
            currentFile?.writeText(editorContent)
            isDirty = false
        }
        currentFile = file
        editorContent = file.readText()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                currentWorkspace = currentWorkspace,
                onFileSelected = { file ->
                    openFile(file)
                    scope.launch { drawerState.close() }
                },
                onWorkspaceChanged = { workspace ->
                    currentWorkspace = workspace
                    // Auto-open entrypoint of new workspace
                    val entrypoint = File(workspace.path, workspace.settings.entrypoint)
                    if (entrypoint.exists()) openFile(entrypoint)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopBar(
                    filename = currentFile?.name ?: "no file",
                    jobStatus = jobStatus,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRunClick = { /* TODO: trigger compile */ }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── Code editor ───────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CodeEditorView(
                        modifier = Modifier.fillMaxSize(),
                        content = editorContent,
                        onContentChange = {
                            editorContent = it
                            isDirty = true
                        }
                    )
                }

                // ── Output panel ──────────────────────────────────────────────
                OutputPanel(
                    text = outputText,
                    expanded = outputExpanded,
                    onToggle = { outputExpanded = !outputExpanded }
                )
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    filename: String,
    jobStatus: JobStatus,
    onMenuClick: () -> Unit,
    onRunClick: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(filename, style = MaterialTheme.typography.titleMedium)
                JobStatusChip(jobStatus)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Open file tree", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(
                onClick = onRunClick,
                enabled = jobStatus is JobStatus.Idle || jobStatus is JobStatus.Done || jobStatus is JobStatus.Error
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Compile and run",
                    tint = if (jobStatus is JobStatus.Idle) LinkBlue else TextMuted
                )
            }
        }
    )
}

// ── Job status chip ───────────────────────────────────────────────────────────

@Composable
fun JobStatusChip(status: JobStatus) {
    when (status) {
        is JobStatus.Idle -> {}
        is JobStatus.Queued -> {
            AssistChip(
                onClick = {},
                label = { Text("Queue: ${status.position}") },
                leadingIcon = {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LinkBlue)
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceNavy, labelColor = TextWhite)
            )
        }
        is JobStatus.Running -> {
            AssistChip(
                onClick = {},
                label = { Text("Compiling...") },
                leadingIcon = {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LinkBlue)
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceNavy, labelColor = TextWhite)
            )
        }
        is JobStatus.Done -> {
            AssistChip(
                onClick = {},
                label = { Text("Done") },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceNavy, labelColor = LinkBlue)
            )
        }
        is JobStatus.Error -> {
            AssistChip(
                onClick = {},
                label = { Text("Error") },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceNavy, labelColor = ErrorRed)
            )
        }
    }
}

// ── Output panel ──────────────────────────────────────────────────────────────

@Composable
fun OutputPanel(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = if (expanded) 200.dp else 40.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OUTPUT", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onToggle) {
                    Text(if (expanded) "∧" else "∨", color = TextMuted)
                }
            }
            if (expanded) {
                Text(
                    text = text.ifEmpty { "No output yet." },
                    style = MonoStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}