package be.bluexin.ghe.view.shell

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import be.bluexin.ghe.io.LayoutLoader
import be.bluexin.ghe.json.*
import be.bluexin.ghe.view.hexedit.HexEditor
import be.bluexin.ghe.view.logger
import be.bluexin.ghe.view.xmledit.XmlEditor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.collectLatest
import java.io.File

val objectMapper = jacksonObjectMapper().apply {
    configure(SerializationFeature.INDENT_OUTPUT, true)
}
val settingsFile = File("settings.json")

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
        settings?.let { LayoutLoader.load<LayoutStructure>(File(it.metadata).parentFile, metadata?.structures) }
            ?.collectLatest {
                structures = it
            }
    }
    var lookups by remember(settings, metadata) {
        mutableStateOf(emptyMap<String, LayoutLookup>())
    }
    LaunchedEffect(settings?.metadata, metadata?.lookups) {
        settings?.let { LayoutLoader.load<LayoutLookup>(File(it.metadata).parentFile, metadata?.lookups) }
            ?.collectLatest {
                lookups = it
            }
    }

    var layouts by remember {
        mutableStateOf(emptyMap<String, DataLayout>())
    }
    LaunchedEffect(settings?.metadata, metadata?.layouts) {
        settings?.let {
            LayoutLoader.load<DataLayout>(File(it.metadata).parentFile, metadata?.layouts) { layout ->
                layout.load(metadata!!, structures, lookups)
            }
        }?.collectLatest {
            layouts = it
        }
    }
    remember(metadata, structures, lookups) {
        metadata?.let { layouts.forEach { (_, v) -> v.load(it, structures, lookups) } }
    }

    fun onLoad(file: File) {
        try {
            settings = Settings(file.canonicalPath, settings?.clientFiles)
            objectMapper.writeValue(settingsFile, settings)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    MaterialTheme(
        colors = if (darkMode.value) darkColors() else lightColors()
    ) {
        val selectedTab = remember { mutableStateOf(AppTabs.XML) }
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                topBar(scaffoldState, scope, selectedTab)
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
            else selectedTab.value.compose(layouts)
        }
    }
}

enum class AppTabs(val displayName: String) {
    HEX("Hex"),
    XML("Xml");

    // Sucks that I can't do this as enum prop, Compose limitation
    @Composable
    fun compose(layouts: Map<String, DataLayout>) {
        when (this) {
            HEX -> {
                HexEditor(layouts)
            }

            XML -> {
                XmlEditor(layouts)
            }
        }
    }
}
