package be.bluexin.ghe.json

data class LayoutStructure(
    var fields: List<Size>,
    override var name: String,
    var size: Int
) : Named