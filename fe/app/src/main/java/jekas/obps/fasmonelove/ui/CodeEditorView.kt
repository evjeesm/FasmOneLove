package jekas.obps.fasmonelove.ui

import android.graphics.Typeface
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat.getFont
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import jekas.obps.fasmonelove.R

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    content: String,
    onContentChange: (String) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CodeEditor(context).apply {
                // ── Color scheme ──────────────────────────────────────────────
                colorScheme = EditorColorScheme().apply {
                    setColor(EditorColorScheme.WHOLE_BACKGROUND,       0xFF0F1E2D.toInt())
                    setColor(EditorColorScheme.TEXT_NORMAL,            0xFFE8E8E8.toInt())
                    setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF0F1E2D.toInt())
                    setColor(EditorColorScheme.LINE_NUMBER,            0xFFAAAAAA.toInt())
                    setColor(EditorColorScheme.CURRENT_LINE,           0xFF1B2B3E.toInt())
                    setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, 0xFF2A3F55.toInt())
                    setColor(EditorColorScheme.SELECTION_INSERT,       0xFF7EB8D4.toInt())
                }

                // Set monospace font
                setTypefaceText(Typeface.MONOSPACE)

                 val typeface = getFont(context, R.font.mononoki_nerd_font_mono_regular)
                 setTypefaceText(typeface)

                setTextSize(16f)
                setText(content)

                subscribeEvent(
                    ContentChangeEvent::class.java
                ) { _, _ ->
                    Log.d("CodeEditorView", "ContentChangeEvent")
                    onContentChange(text.toString())
                }
            }
        },
        update = { editor ->
            editor.typefaceText = Typeface.MONOSPACE
            Log.d("CodeEditorView", "Update")
            if (editor.text.toString() != content) {
                editor.setText(content)
            }
        }
    )
}