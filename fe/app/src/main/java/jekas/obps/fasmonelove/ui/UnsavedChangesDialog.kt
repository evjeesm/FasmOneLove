package jekas.obps.fasmonelove.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class UnsavedChangesDialogValues {
    SAVE_CHANGES,
    DISCARD_CHANGES
}

class UnsavedChangesDialogController {

    private var continuation:
            CancellableContinuation<UnsavedChangesDialogValues>? = null

    var visible by mutableStateOf(false)
        private set

    var bufferName by mutableStateOf<String?>("")
        private set

    suspend fun awaitResult(bufferName: String?): UnsavedChangesDialogValues =
        suspendCancellableCoroutine { cont ->
            continuation = cont
            visible = true
            this.bufferName = bufferName
        }

    fun confirm() {
        visible = false
        continuation?.resume(UnsavedChangesDialogValues.SAVE_CHANGES)
        continuation = null
    }

    fun dismiss() {
        visible = false
        continuation?.resume(UnsavedChangesDialogValues.DISCARD_CHANGES)
        continuation = null
    }
}

@Composable
fun UnsavedChangesDialog(controller: UnsavedChangesDialogController) {
    if (controller.visible) {
        AlertDialog(
            onDismissRequest = { controller.dismiss() },

            title = {
                Text("Unsaved changes in \"${controller.bufferName ?: "<UNTITLED>"}\"")
            },

            text = {
                Text("Do you want to save your changes?")
            },

            confirmButton = {
                TextButton(onClick = { controller.confirm() }) {
                    Text("Save")
                }
            },

            dismissButton = {
                TextButton(onClick = { controller.dismiss() }) {
                    Text("Discard")
                }
            }
        )
    }
}