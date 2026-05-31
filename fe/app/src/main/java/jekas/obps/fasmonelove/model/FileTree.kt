package jekas.obps.fasmonelove.model

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService

class FileTree {
    val files: MutableList<File> = mutableStateListOf()

    private val watchService: WatchService

    private val root: Path

    constructor(root: Path)
    {
        this.root = root
        this.watchService = FileSystems.getDefault().newWatchService()
        root.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
    }

    suspend fun watch()
    {
        while (true)
        {
            files.clear()
            _watch().collect { path ->  files.add(path.toFile()) }
        }
    }

    private fun _watch(): Flow<Path> = callbackFlow {
        val job = launch(Dispatchers.IO) {

            while (isActive) {

                val key = watchService.take()

                for (event in key.pollEvents()) {

                    val relative =
                        event.context() as Path

                    trySend(root.resolve(relative))
                }

                key.reset()
            }
        }

        awaitClose {
            job.cancel()
            watchService.close()
        }
    }
}

