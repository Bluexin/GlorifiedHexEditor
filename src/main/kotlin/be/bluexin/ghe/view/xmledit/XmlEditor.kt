package be.bluexin.ghe.view.xmledit

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import be.bluexin.ghe.io.DataFile
import be.bluexin.layoutloader.DataLayout

@Composable
fun XmlEditor(layouts: Map<String, DataLayout>, data: DataFile?) {
    Column {
        Text("TODO: pick layout/file (rn hardcoded to datafile_111/itembuypricedata)")
        TableScreen(layouts["datafile_111.xml"], data)
    }
}
