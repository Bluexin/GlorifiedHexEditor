package be.bluexin.ghe.view.shell

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import be.bluexin.ghe.view.hexedit.HexEditor
import be.bluexin.ghe.view.logger
import be.bluexin.ghe.view.xmledit.XmlEditor
import be.bluexin.layoutloader.DataFile
import be.bluexin.layoutloader.DataFileHandler
import be.bluexin.layoutloader.LayoutLoader
import be.bluexin.layoutloader.json.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
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
        logger.info { "Reading structures" }
        settings?.let { LayoutLoader.load<LayoutStructure>(File(it.metadata).parentFile, metadata?.structures) }
            ?.collectLatest {
                logger.info { "Read structures ${it.keys}" }
                structures = it
            }
    }
    var lookups by remember(settings, metadata) {
        mutableStateOf(emptyMap<String, LayoutLookup>())
    }
    LaunchedEffect(settings?.metadata, metadata?.lookups) {
        logger.info { "Reading lookups" }
        settings?.let { LayoutLoader.load<LayoutLookup>(File(it.metadata).parentFile, metadata?.lookups) }
            ?.collectLatest {
                logger.info { "Read lookups ${it.keys}" }
                lookups = it
            }
    }

    var layouts by remember {
        mutableStateOf(emptyMap<String, DataLayout>())
    }
    LaunchedEffect(structures, lookups) {
        logger.info { "Reading layouts" }
        if (structures.isEmpty() or lookups.isEmpty()) {
            logger.info { "Skipped due to missing structures / lookups" }
        } else settings?.let {
            LayoutLoader.load<DataLayout>(File(it.metadata).parentFile, metadata?.layouts) { layout ->
                layout.load(metadata!!, structures, lookups)
            }
        }?.collectLatest {
            logger.info { "Read layouts ${it.keys}" }
            layouts = it
        }
    }

    var data by remember(layouts) { mutableStateOf<DataFile?>(null) }
    LaunchedEffect(layouts) {
        if (layouts.isEmpty()) {
            logger.info { "Skipping loading data, as layouts are not loaded yet" }
        } else {
            val (loaded, updates) = DataFileHandler.loadData("/home/bluexin/Rebellion/XML.DAT_CLIENT_FILES/datafile.bin.files/datafile_111.xml")
                ?: Pair(DataFile("", emptyList()), emptyFlow())
            data = loaded
            updates.collect {
                // TODO : ask user whether to reload file
            }
        }
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
            else when (selectedTab.value) {
                // Sucks that I can't do this as enum prop, Compose limitation
                AppTabs.HEX -> HexEditor(layouts)
                AppTabs.XML -> XmlEditor(layouts, data)
            }
        }
    }
}

enum class AppTabs(val displayName: String) {
    HEX("Hex"),
    XML("Xml");
}
