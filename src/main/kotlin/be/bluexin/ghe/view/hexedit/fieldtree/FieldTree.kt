package be.bluexin.ghe.view.hexedit.fieldtree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import be.bluexin.layoutloader.DataLayout
import be.bluexin.layoutloader.Field
import be.bluexin.layoutloader.Repeated

class ExpandableField(
    val field: Field,
    val level: Int,
    val idx: Int = -1
) {
    var children: List<ExpandableField> by mutableStateOf(emptyList())
    val canExpand: Boolean get() = idx == -1 && this.field is Repeated
    val isExpanded: Boolean get() = children.isNotEmpty()

    fun toggleExpanded() {
        children = if (children.isEmpty()) {
            val repeated = field as Repeated
            List(repeated.repeat) {
                ExpandableField(field, level + 1, it)
            }
        } else emptyList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExpandableField) return false

        if (field != other.field) return false
        if (level != other.level) return false
        if (idx != other.idx) return false

        return true
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + level
        result = 31 * result + idx
        return result
    }
}

class FieldTree(val layout: DataLayout) {
    private val topLevel = layout.fields.map { ExpandableField(it, 0) }

    val items: List<ExpandableField> get() = topLevel.asSequence().flatMap { listOf(it) + it.children }.toList()
}
