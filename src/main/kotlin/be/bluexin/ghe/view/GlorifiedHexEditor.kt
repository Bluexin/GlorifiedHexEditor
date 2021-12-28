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
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import compose.icons.evaicons.OutlineGroup
import compose.icons.evaicons.outline.Menu
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.File

typealias AppIcons = OutlineGroup

val objectMapper = jacksonObjectMapper().apply {
    configure(SerializationFeature.INDENT_OUTPUT, true)
}
val settingsFile = File("settings.json")

val logger = KotlinLogging.logger {}

private fun loadLayouts(
    root: File,
    metadata: Metadata?,
    structures: Map<String, LayoutStructure>,
    lookups: Map<String, LayoutLookup>
): Map<String, DataLayout> {
    if (metadata == null) return emptyMap()
    logger.info { "loading ${DataLayout::class.simpleName}" }
    return File(root, metadata.layouts).walk().filter { !it.isDirectory }.mapNotNull {
        try {
            val layout = objectMapper.readValue<DataLayout>(it)
            layout.load(metadata, structures, lookups)
            layout
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load $it" }
            null
        }
    }.associateBy(DataLayout::fileFilter)
}

private inline fun <reified T : Named> load(root: File, directory: String?): Map<String, T> {
    if (directory == null) return emptyMap()
    logger.info { "Loading ${T::class.simpleName}" }
    return File(root, directory).walk().filter { !it.isDirectory }.mapNotNull {
        try {
            objectMapper.readValue<T>(it)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load $it" }
            null
        }
    }.associateBy(Named::name)
}

private fun loadStructures(root: File, metadata: Metadata?): Map<String, LayoutStructure> =
    load(root, metadata?.structures)

private fun loadLookups(root: File, metadata: Metadata?): Map<String, LayoutLookup> = load(root, metadata?.lookups)

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
    var darkMode by remember { mutableStateOf(true) }
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(loadSettings()) }
    val metadata = remember(settings) {
        settings?.let { objectMapper.readValue<Metadata>(File(it.metadata)) }
    }
    val structures = remember(settings, metadata) {
        settings?.let { loadStructures(File(it.metadata).parentFile, metadata) } ?: emptyMap()
    }
    val lookups = remember(settings, metadata) {
        settings?.let { loadLookups(File(it.metadata).parentFile, metadata) } ?: emptyMap()
    }
    val layouts = remember(settings, metadata, structures, lookups) {
        settings?.let { loadLayouts(File(it.metadata).parentFile, metadata, structures, lookups) } ?: emptyMap()
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
        colors = if (darkMode) darkColors() else lightColors()
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Glorified Hex Editor") },
                    elevation = 8.dp,
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { scaffoldState.drawerState.open() }
                        }) {
                            Icon(AppIcons.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            drawerContent = {
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                ) {
                    LoadButton(settings?.metadata, ::onLoad)
                    DarkLightSwitch(darkMode) { darkMode = it }
                }
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
