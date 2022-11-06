package be.bluexin.ghe.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.bluexin.ghe.json.*
import be.bluexin.ghe.loader.Loader
import be.bluexin.ghe.view.hexedit.Editor
import be.bluexin.ghe.view.menu.LoadButton
import be.bluexin.ghe.view.menu.menuContent
import be.bluexin.ghe.view.menu.topBar
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.io.File

val objectMapper = jacksonObjectMapper().apply {
    configure(SerializationFeature.INDENT_OUTPUT, true)
}
val settingsFile = File("settings.json")

val logger = KotlinLogging.logger {}

private fun loadSettings(): Settings? {
    return if (settingsFile.isFile) try {
        objectMapper.readValue<Settings>(settingsFile)
    } catch (e: Exception) {
        logger.warn(e) { "Couldn't load settings" }
        null
    } else null
}

@Composable
@Preview
fun FrameWindowScope.App() {
    val darkMode = remember { mutableStateOf(true) }
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(loadSettings()) }
    val metadata = remember(settings) {
        settings?.let { objectMapper.readValue<Metadata>(File(it.metadata)) }
    }
    var structures by remember(settings, metadata) {
        mutableStateOf(emptyMap<String, LayoutStructure>())
    }
    LaunchedEffect(settings?.metadata, metadata?.structures) {
        settings?.let { Loader.load<LayoutStructure>(File(it.metadata).parentFile, metadata?.structures) }?.collect {
            structures = it
        }
    }
    var lookups by remember(settings, metadata) {
        mutableStateOf(emptyMap<String, LayoutLookup>())
    }
    LaunchedEffect(settings?.metadata, metadata?.lookups) {
        settings?.let { Loader.load<LayoutLookup>(File(it.metadata).parentFile, metadata?.lookups) }?.collect {
            lookups = it
        }
    }

    var layouts by remember {
        mutableStateOf(emptyMap<String, DataLayout>())
    }
    LaunchedEffect(settings?.metadata, metadata?.layouts) {
        settings?.let {
            Loader.load<DataLayout>(File(it.metadata).parentFile, metadata?.layouts) { layout ->
                layout.load(metadata!!, structures, lookups)
            }
        }?.collect {
            layouts = it
        }
    }
    remember(metadata, structures, lookups) {
        metadata?.let { layouts.forEach { (_, v) -> v.load(it, structures, lookups) } }
    }

    fun onLoad(file: File) {
        try {
            settings = Settings(file.canonicalPath)
            objectMapper.writeValue(settingsFile, settings)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    MaterialTheme(
        colors = if (darkMode.value) darkColors() else lightColors()
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                topBar(scaffoldState, scope)
            },
            drawerContent = {
                menuContent(settings, darkMode, ::onLoad)
            },
            drawerShape = MaterialTheme.shapes.medium,
            floatingActionButton = {
                if (layouts.isEmpty()) LoadButton(settings?.metadata, ::onLoad)
            }
        ) {
            if (layouts.isEmpty()) Column(Modifier.padding(8.dp)) { Text("Please load mappings") }
            else Editor(layouts)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Glorified Hex Editor") {
        App()
    }
}
