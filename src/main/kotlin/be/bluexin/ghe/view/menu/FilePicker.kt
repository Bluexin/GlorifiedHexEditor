package be.bluexin.ghe.view.menu

import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.io.File

fun openFileDialog(
    window: ComposeWindow,
    title: String,
    allowedExtensions: List<String>,
    allowMultiSelection: Boolean = true,
    directory: String? = null
): Set<File> {
    return FileDialog(window, title, FileDialog.LOAD).apply {
        isMultipleMode = allowMultiSelection

        // windows
        file = allowedExtensions.joinToString(";") { "*$it" }

        // linux
        setFilenameFilter { _, name ->
            allowedExtensions.any(name::endsWith)
        }

        if (directory != null) {
            val f = File(directory)
            this.directory = if (f.isDirectory) f.canonicalPath else f.parent
        }

        toFront()
        isVisible = true
    }.files.toSet()
}