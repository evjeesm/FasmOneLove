package jekas.obps.fasmonelove.model

import android.util.Log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class TextBuffer {
    private var file: File?

    // Create unnamed buffer
    constructor() {
        this.file = null
        this._content = ""
        this.isDirty = false
    }

    constructor(path: String) {
        this.file = File(path)
        this._content = if (this.file!!.exists()) this.file!!.readText() else ""
        this.isDirty = false
    }

    var content: String
        get() {
            return this._content
        }
        set(value) {
            if (this._content != value) {
                isDirty = true
                this._content = value
                Log.d("TextBuffer", "Dirty Set!")
                Log.d("TextBuffer", "set content from \"${this._content}\" to \"$value\"")
            }
        }


    val name: String
        get() {
            return this.file?. let { file!!.name } ?: "Untitled"
        }

    val path: String?
        get() = this.file?.path

    var isDirty: Boolean
        get() = this._isDirty
        private set(value) { this._isDirty = value }

    private var _isDirty by mutableStateOf(false)
    private var _content by mutableStateOf<String>("")

    // Best-effort save, returns false if buffer is not backed by a file
    fun save(): Boolean {
        if (this.file == null) return false

        // nothing to save
        if (!this.isDirty) return true

        // save changes
        this.file!!.writeText(this.content)
        isDirty = false
        return true
    }

    // Changes target file of the buffer to sync with,
    // Save _content to an old file before calling this method
    fun setFile(path: String) {
        this.file = File(path)
        this.isDirty = true
    }

    // Saves buffer's content in a new file.
    // Buffer still points to an old file and dirty flag is not cleared!
    fun saveAs(path: String) {
        File(path).writeText(this.content)
    }

    fun reload() {
        if (file == null) {
            content = ""
            this.isDirty = false
            return
        }
        this._content = file!!.readText()
        this.isDirty = false
    }
}