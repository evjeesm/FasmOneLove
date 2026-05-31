package jekas.obps.fasmonelove.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.model.FileOperations
import jekas.obps.fasmonelove.model.Workspace
import jekas.obps.fasmonelove.ui.theme.BorderNavy
import jekas.obps.fasmonelove.ui.theme.ErrorRed
import jekas.obps.fasmonelove.ui.theme.LinkBlue
import jekas.obps.fasmonelove.ui.theme.SurfaceNavy
import jekas.obps.fasmonelove.ui.theme.TextMuted
import jekas.obps.fasmonelove.ui.theme.TextWhite
import java.io.File

@Composable
fun FileBrowser(
    currentWorkspace: Workspace,
    onFileSelected: (File) -> Unit,
) {
    val workspaceDir = File(currentWorkspace.path)
    val outputDir = File(currentWorkspace.path, "output")
    var refreshKey by remember { mutableStateOf(0) }
    var outputExpanded by remember { mutableStateOf(false) }

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


    Column {
        LazyColumn (modifier = Modifier.weight(0.5f)) {
            // ── Sources section ───────────────────────────────────────────
            item {
                SectionHeaderWithAdd(
                    title = "SOURCES",
                    onNewFile = { name ->
                        FileOperations.createFile(workspaceDir, name)
                        refreshKey++
                    }
                )
            }
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
        LazyColumn (modifier = Modifier.run { defaultMinSize(minHeight = 50.dp).weight(if (outputExpanded) 0.5f else 0.075f) }) {
            // ── Output section ────────────────────────────────────────────
            item {
                HorizontalDivider(
                    color = BorderNavy,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                SectionHeader(
                    title = "OUTPUT",
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

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Output,
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Source,
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
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
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

