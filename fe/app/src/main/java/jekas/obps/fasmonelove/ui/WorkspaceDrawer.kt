package jekas.obps.fasmonelove.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jekas.obps.fasmonelove.model.Workspace
import jekas.obps.fasmonelove.model.WorkspaceManager
import jekas.obps.fasmonelove.ui.theme.*

@Composable
fun WorkspaceDrawer(
    onWorkspaceOpened: (Workspace) -> Unit,
) {
    val context = LocalContext.current
    var workspaces by remember { mutableStateOf(WorkspaceManager.listWorkspaces(context)) }
    var showNewDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WORKSPACE",
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New workspace", tint = LinkBlue)
            }
        }

        HorizontalDivider(color = BorderNavy)

        // ── Empty state ───────────────────────────────────────────────────────
        if (workspaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { showNewDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceNavy,
                        contentColor = LinkBlue,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Workspace")
                }
            }
        } else {
            // ── Workspace list ────────────────────────────────────────────────
            workspaces.forEach { workspace ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            workspace.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    selected = false,
                    onClick = {
                        WorkspaceManager.saveLastOpened(context, workspace.name)
                        onWorkspaceOpened(workspace)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.surface,
                        unselectedTextColor = TextWhite,
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }

    // ── New workspace dialog ──────────────────────────────────────────────────
    if (showNewDialog) {
        NewWorkspaceDialog(
            onConfirm = { name ->
                val workspace = WorkspaceManager.createWorkspace(context, name)
                workspaces = WorkspaceManager.listWorkspaces(context)
                showNewDialog = false
                onWorkspaceOpened(workspace)
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
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = ""
                    },
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
            }
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