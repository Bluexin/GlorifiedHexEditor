package be.bluexin.ghe.view.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import java.io.File

@Composable
fun FrameWindowScope.LoadButton(directory: String?, onLoad: (File) -> Unit) {
    Button(onClick = {
        openFileDialog(window, "Select mapping metadata", listOf("metadata.json"), false, directory)
            .singleOrNull()?.apply(onLoad)
    }, modifier = Modifier.padding(2.dp)) {
        Text("Load mappings")
    }
}
