package jekas.obps.fasmonelove.model

import java.io.File

object FileOperations {

    fun createFile(parent: File, name: String): File {
        val file = File(parent, name)
        file.createNewFile()
        return file
    }

    fun deleteFile(file: File) {
        if (file.isDirectory) file.deleteRecursively()
        else file.delete()
    }

    fun renameFile(file: File, newName: String): File {
        val dest = File(file.parent, newName)
        file.renameTo(dest)
        return dest
    }

    fun duplicateFile(file: File): File {
        val name = file.nameWithoutExtension
        val ext = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var dest = File(file.parent, "${name}_copy${ext}")
        var i = 2
        while (dest.exists()) {
            dest = File(file.parent, "${name}_copy$i${ext}")
            i++
        }
        file.copyTo(dest)
        return dest
    }
}