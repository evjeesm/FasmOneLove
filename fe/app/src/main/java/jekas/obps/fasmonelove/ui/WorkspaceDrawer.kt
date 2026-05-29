package jekas.obps.fasmonelove.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.WorkspaceSettingsActivity
import jekas.obps.fasmonelove.model.FileOperations
import jekas.obps.fasmonelove.model.Workspace
import jekas.obps.fasmonelove.model.WorkspaceManager
import jekas.obps.fasmonelove.ui.theme.*
import java.io.File

@Composable
fun WorkspaceDrawer(
    currentWorkspace: Workspace?,
    onFileSelected: (File) -> Unit,
    onWorkspaceChanged: (Workspace) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var sourcesExpanded by remember { mutableStateOf(true) }
    var outputExpanded by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentWorkspace?.name ?: "WORKSPACE",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )

            // Settings — opens WorkspaceSettingsActivity
            IconButton(
                onClick = {
                    currentWorkspace?.let {
                        val intent = Intent(context, WorkspaceSettingsActivity::class.java)
                        intent.putExtra("workspace_name", it.name)
                        intent.putExtra("workspace_path", it.path)
                        context.startActivity(intent)
                    }
                },
                enabled = currentWorkspace != null,
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Workspace settings", tint = if (currentWorkspace != null) TextMuted else BorderNavy)
            }

            // Folder — opens workspace picker
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Switch workspace", tint = LinkBlue)
            }
        }

        HorizontalDivider(color = BorderNavy)

        if (currentWorkspace == null) {
            // ── No workspace ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { showPicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceNavy,
                        contentColor = LinkBlue,
                    ),
                    border = BorderStroke(1.dp, BorderNavy),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Workspace")
                }
            }
        } else {
            val workspaceDir = File(currentWorkspace.path)
            val outputDir = File(currentWorkspace.path, "output")

            val sourceFiles = remember(refreshKey) {
                workspaceDir.listFiles()
                    ?.filter { it.name != "output" && it.name != ".workspace.json" }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                    ?: emptyList()
            }

            val outputFiles = outputDir
                .listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                ?: emptyList()

            LazyColumn {
                // ── Sources section ───────────────────────────────────────────
                item {
                    SectionHeaderWithAdd(
                        title = "SOURCES",
                        expanded = sourcesExpanded,
                        onToggle = { sourcesExpanded = !sourcesExpanded },
                        onNewFile = { name ->
                            FileOperations.createFile(workspaceDir, name)
                            refreshKey++
                        }
                    )
                }
                if (sourcesExpanded) {
                    items(sourceFiles) { file ->
                        FileTreeItemWithMenu(
                            file = file,
                            depth = 0,
                            onFileSelected = onFileSelected,
                            onFileDeleted = { f ->
                                FileOperations.deleteFile(f)
                                refreshKey++
                            },
                            onFileDuplicated = { f ->
                                FileOperations.duplicateFile(f)
                                refreshKey++
                            },
                            onFileRenamed = { f, newName ->
                                FileOperations.renameFile(f, newName)
                                refreshKey++
                            }
                        )
                    }
                }

                // ── Output section ────────────────────────────────────────────
                item {
                    HorizontalDivider(
                        color = BorderNavy,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    SectionHeader(
                        title = "OUTPUT",
                        expanded = outputExpanded,
                        onToggle = { outputExpanded = !outputExpanded }
                    )
                }
                if (outputExpanded) {
                    if (outputFiles.isEmpty()) {
                        item {
                            Text(
                                "No output yet",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
                            )
                        }
                    } else {
                        items(outputFiles) { file ->
                            FileTreeItem(
                                file = file,
                                depth = 0,
                                onFileSelected = onFileSelected,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Workspace picker ──────────────────────────────────────────────────────
    if (showPicker) {
        WorkspacePickerDialog(
            onWorkspaceSelected = { workspace ->
                onWorkspaceChanged(workspace)
                refreshKey++
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SectionHeaderWithAdd(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNewFile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = SurfaceNavy,
        ) {
            DropdownMenuItem(
                text = { Text("New File", color = TextWhite) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted) },
                onClick = { showMenu = false; showNewFileDialog = true }
            )
        }
    }

    if (showNewFileDialog) {
        NewFileDialog(
            title = "New File",
            initialValue = "untitled.asm",
            confirmLabel = "Create",
            onConfirm = { name -> showNewFileDialog = false; onNewFile(name) },
            onDismiss = { showNewFileDialog = false }
        )
    }

}

// ── File tree item ────────────────────────────────────────────────────────────

@Composable
fun FileTreeItem(file: File, depth: Int, onFileSelected: (File) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (file.isDirectory) expanded = !expanded
                else onFileSelected(file)
            }
            .padding(
                start = (16 + depth * 12).dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (file.isDirectory) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Spacer(Modifier.width(14.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = if (file.isDirectory) LinkBlue else TextWhite,
            ),
        )
    }
}

// ── File context menu ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItemWithMenu(
    file: File,
    depth: Int,
    onFileSelected: (File) -> Unit,
    onFileDeleted: (File) -> Unit,
    onFileDuplicated: (File) -> Unit,
    onFileRenamed: (File, String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (file.isDirectory) expanded = !expanded
                        else onFileSelected(file)
                    },
                    onLongClick = { showMenu = true }
                )
                .padding(
                    start = (16 + depth * 12).dp,
                    top = 6.dp,
                    bottom = 6.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (file.isDirectory) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Spacer(Modifier.width(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (file.isDirectory) LinkBlue else TextWhite,
                ),
            )
        }

        // ── Context menu ──────────────────────────────────────────────────────
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = SurfaceNavy,
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = TextWhite) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) },
                onClick = { showMenu = false; showRenameDialog = true }
            )
            DropdownMenuItem(
                text = { Text("Duplicate", color = TextWhite) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextMuted) },
                onClick = {
                    showMenu = false
                    onFileDuplicated(file)
                }
            )
            HorizontalDivider(color = BorderNavy)
            DropdownMenuItem(
                text = { Text("Delete", color = ErrorRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                onClick = { showMenu = false; showDeleteDialog = true }
            )
        }
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    if (showRenameDialog) {
        NewFileDialog(
            title = "Rename",
            initialValue = file.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                showRenameDialog = false
                onFileRenamed(file, newName)
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceNavy,
            title = { Text("Delete ${file.name}?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    if (file.isDirectory) "This will delete the folder and all its contents."
                    else "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onFileDeleted(file) }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}






// ── Workspace picker dialog ───────────────────────────────────────────────────

@Composable
fun WorkspacePickerDialog(
    onWorkspaceSelected: (Workspace) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var workspaces by remember { mutableStateOf(WorkspaceManager.listWorkspaces(context)) }
    var showNewDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceNavy,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Workspaces", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showNewDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New workspace", tint = LinkBlue)
                }
            }
        },
        text = {
            if (workspaces.isEmpty()) {
                Text(
                    "No workspaces yet. Create one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            } else {
                LazyColumn {
                    items(workspaces) { workspace ->
                        Text(
                            text = workspace.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWorkspaceSelected(workspace) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                        HorizontalDivider(color = BorderNavy)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )

    if (showNewDialog) {
        NewWorkspaceDialog(
            onConfirm = { name ->
                val workspace = WorkspaceManager.createWorkspace(context, name)
                workspaces = WorkspaceManager.listWorkspaces(context)
                showNewDialog = false
                onWorkspaceSelected(workspace)
            },
            onDismiss = { showNewDialog = false }
        )
    }
}

// ── New workspace dialog ──────────────────────────────────────────────────────

@Composable
fun NewWorkspaceDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceNavy,
        title = {
            Text("New Workspace", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = "" },
                label = { Text("Name") },
                singleLine = true,
                isError = error.isNotEmpty(),
                supportingText = if (error.isNotEmpty()) {
                    { Text(error, color = ErrorRed) }
                } else null,
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        name.isBlank() -> error = "Name cannot be empty"
                        name.contains("/") || name.contains("\\") -> error = "Invalid characters"
                        else -> onConfirm(name.trim())
                    }
                }
            ) {
                Text("Create", color = LinkBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}