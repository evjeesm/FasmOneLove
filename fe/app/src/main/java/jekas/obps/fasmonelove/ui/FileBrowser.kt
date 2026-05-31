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
import jekas.obps.fasmonelove.model.Workspace
import jekas.obps.fasmonelove.ui.theme.BorderNavy
import jekas.obps.fasmonelove.ui.theme.ErrorRed
import jekas.obps.fasmonelove.ui.theme.LinkBlue
import jekas.obps.fasmonelove.ui.theme.SurfaceNavy
import jekas.obps.fasmonelove.ui.theme.TextMuted
import jekas.obps.fasmonelove.ui.theme.TextWhite
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FileBrowser(
    currentWorkspace: Workspace,
    onFileSelected: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val workspaceDir = File(currentWorkspace.path)
    val outputDir = File(currentWorkspace.path, "output")


    var outputExpanded by remember { mutableStateOf(false) }

//    val sourceFiles = remember() {
//        workspaceDir.listFiles()
//            ?.filter { it.name != "output" && it.name != ".workspace.json" }
//            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
//            ?: emptyList()
//    }
//
    val outputFiles = outputDir
        .listFiles()
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        ?: emptyList()


    Column {
//        LazyColumn (modifier = Modifier.weight(0.5f)) {
//            // ── Sources section ───────────────────────────────────────────
//            item {
//                SectionHeaderWithAdd(
//                    title = "SOURCES",
//                    onNewFile = { name ->
//                        FileOperations.createFile(workspaceDir, name)
//                    }
//                )
//            }
//            items(sourceFiles) { file ->
//                FileTreeItemWithMenu(
//                    file = file,
//                    depth = 0,
//                    onFileSelected = onFileSelected,
//                    onFileDeleted = { f ->
//                        FileOperations.deleteFile(f)
//                    },
//                    onFileDuplicated = { f ->
//                        FileOperations.duplicateFile(f)
//                    },
//                    onFileRenamed = { f, newName ->
//                        FileOperations.renameFile(f, newName)
//                    }
//                )
//            }
//        }
        FileTreeView(workspaceDir.toPath(), onFileSelected)
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



