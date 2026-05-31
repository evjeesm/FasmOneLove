package jekas.obps.fasmonelove.filetree

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService

class FileTreeViewModel
{
    private val _state : FileTreeState

    val onFileSelected: (File) -> Unit

    constructor(
        root: File,
        hide: List<String>?,
        onFileSelected: (File) -> Unit,
    ) {
        _state = FileTreeState(
            tree = root,
            files = (root.listFiles()
                ?.filter { it.name != "output" && it.name != ".workspace.json" }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                ?: emptyList()).toMutableStateList(),
            hide = hide,
        )

        this.onFileSelected = onFileSelected
    }

    val files: MutableList<File>
        get() = _state.files

    val root: File?
        get() = _state.tree

    suspend fun watch()
    {
        while (true)
        {
            if (_state.tree == null) continue
            _watch(_state.tree.toPath()).collect { event ->
                val relative = event?.context() as Path
                val file = _state.tree.toPath().resolve(relative).toFile()

                when (event.kind()) {
                    StandardWatchEventKinds.ENTRY_CREATE -> {
                        _state.files.add(file)
                    }

                    StandardWatchEventKinds.ENTRY_DELETE -> {
                        _state.files.remove(file)
                    }

                    StandardWatchEventKinds.ENTRY_MODIFY -> {
                        Log.d("FileTree", "file Modified")
                    }
                }
            }
        }
    }


    private fun _watch(path: Path): Flow<WatchEvent<*>?> = callbackFlow {
        val watchService: WatchService = FileSystems.getDefault().newWatchService()

        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        val job = launch(Dispatchers.IO) {

            while (isActive) {

                val key = watchService.take()

                for (event in key.pollEvents()) {
//                    val relative = event.context() as Path
                    trySend(event)
                }

                key.reset()
            }
        }

        awaitClose {
            job.cancel()
            watchService.close()
        }
    }
        .flowOn(Dispatchers.IO)
}