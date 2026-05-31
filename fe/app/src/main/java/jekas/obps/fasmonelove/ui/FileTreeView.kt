package jekas.obps.fasmonelove.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.model.FileOperations
import jekas.obps.fasmonelove.model.FileTree
import jekas.obps.fasmonelove.ui.theme.BorderNavy
import jekas.obps.fasmonelove.ui.theme.ErrorRed
import jekas.obps.fasmonelove.ui.theme.LinkBlue
import jekas.obps.fasmonelove.ui.theme.SurfaceNavy
import jekas.obps.fasmonelove.ui.theme.TextMuted
import jekas.obps.fasmonelove.ui.theme.TextWhite
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory

@Composable
fun FileTreeView(
    root: Path,
    onFileSelected: (File) -> Unit
) {
    val scope = rememberCoroutineScope()
    assert(root.isDirectory())

    val tree by remember { mutableStateOf( FileTree(root) ) }


    LaunchedEffect(Unit) {
        scope.launch { tree.watch() }
    }

    LazyColumn {
        // ── Sources section ───────────────────────────────────────────
        item {
            SectionHeaderWithAdd(
                title = "SOURCES",
                onNewFile = { name ->
                    FileOperations.createFile(root.toFile(), name)
                }
            )
        }
        items(tree.files.toList()) { file ->
            FileTreeItemWithMenu(
                file = file,
                depth = 0,
                onFileSelected = onFileSelected,
                onFileDeleted = { f ->
                    FileOperations.deleteFile(f)
                },
                onFileDuplicated = { f ->
                    FileOperations.duplicateFile(f)
                },
                onFileRenamed = { f, newName ->
                    FileOperations.renameFile(f, newName)
                }
            )
        }
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