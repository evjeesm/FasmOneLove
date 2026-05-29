package jekas.obps.fasmonelove.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jekas.obps.fasmonelove.ui.theme.BorderNavy
import jekas.obps.fasmonelove.ui.theme.ErrorRed
import jekas.obps.fasmonelove.ui.theme.LinkBlue
import jekas.obps.fasmonelove.ui.theme.SurfaceNavy
import jekas.obps.fasmonelove.ui.theme.TextMuted
import jekas.obps.fasmonelove.ui.theme.TextWhite
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ── Reusable filename input dialog ────────────────────────────────────────────

class NewFileDialogController
{
    enum class Values {
        CONFIRM,
        DISMISS
    }

    data class ValuesWithFilename(
        val value: Values = Values.DISMISS,
        val filename: String = ""
    )

    private var continuation:
            CancellableContinuation<ValuesWithFilename>? = null

    var visible by mutableStateOf(false)
        private set

    suspend fun awaitResult(): ValuesWithFilename =
        suspendCancellableCoroutine { cont ->
            continuation = cont
            visible = true
        }

    fun confirm(filename: String) {
        visible = false
        continuation?.resume(ValuesWithFilename(Values.CONFIRM, filename))
        continuation = null
    }

    fun dismiss() {
        visible = false
        continuation?.resume(ValuesWithFilename(Values.DISMISS))
        continuation = null
    }
}

@Composable
fun InvokableNewFileDialog(controller: NewFileDialogController,
   title: String,
   initialValue: String,
   confirmLabel: String)
{
    if (controller.visible)
    {
        NewFileDialog(
            title,
            initialValue,
            confirmLabel,
            onConfirm = { name -> controller.confirm(name) },
            onDismiss = { controller.dismiss() },
            )
    }
}

@Composable
fun NewFileDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceNavy,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
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
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = "Name cannot be empty"
                    name.contains("/") || name.contains("\\") -> error = "Invalid characters"
                    else -> onConfirm(name.trim())
                }
            }) {
                Text(confirmLabel, color = LinkBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}