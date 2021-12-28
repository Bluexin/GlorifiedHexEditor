package be.bluexin.ghe.json

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore

class DataLayout(
    var fields: List<Field>,
) {
    @JsonAnyGetter
    @JsonAnySetter
    val extra: MutableMap<String, String> = mutableMapOf()

    @JsonIgnore
    lateinit var fileFilter: String
        private set

    fun load(metadata: Metadata, structures: Map<String, LayoutStructure>, lookups: Map<String, LayoutLookup>) {
        fileFilter = extra[metadata.fileFilter] ?: error("Couldn't find file filter in $extra")
        fields.forEach {
            @Suppress("NON_EXHAUSTIVE_WHEN_STATEMENT")
            when (it) {
                is Structure -> it.structureRef = structures[it.structure]
                    ?: error("Missing referenced structure ${it.structure}")
                is Lookup -> it.lookupRef = lookups[it.lookup]
                    ?: error("Missing referenced lookup ${it.lookup}")
            }
        }
    }
}