package jekas.obps.fasmonelove.filetree

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File

data class FileTreeState (
    val tree: File? = null,
    val pointer: File? = null, // points to one of the root children or root itself,
                               // to show part of a tree

    val hide: List<String>? = null, // filter out these files
    val files: SnapshotStateList<File> = mutableStateListOf()
)