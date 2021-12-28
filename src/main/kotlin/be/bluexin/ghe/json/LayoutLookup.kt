package be.bluexin.ghe.json

data class LayoutLookup(
    override var name: String,
    var size: Int,
    val values: MutableList<String>
) : Named