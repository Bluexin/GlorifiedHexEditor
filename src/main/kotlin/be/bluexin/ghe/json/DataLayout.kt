package be.bluexin.ghe.json

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.jetbrains.skiko.currentNanoTime

class DataLayout(
    var fields: List<Field>,
) : Named {
    @JsonAnyGetter
    @JsonAnySetter
    val extra: MutableMap<String, String> = mutableMapOf()

    @JsonIgnore
    lateinit var fileFilter: String
        private set

    var loadingTimeStamp = 0L
        private set

    fun load(metadata: Metadata, structures: Map<String, LayoutStructure>, lookups: Map<String, LayoutLookup>) {
        fileFilter = extra[metadata.fileFilter] ?: error("Couldn't find file filter in $extra")
        logger.info { "Resolving $name" }
        fields.forEach {
            try {
                @Suppress("NON_EXHAUSTIVE_WHEN_STATEMENT")
                when (it) {
                    is Structure -> it.structureRef = structures[it.structure]
                        ?: error("Missing referenced structure ${it.structure}")
                    is Lookup -> it.lookupRef = lookups[it.lookup]
                        ?: error("Missing referenced lookup ${it.lookup}")
                }
            } catch (e: Exception) {
                logger.info { "\t${e.message}" }
            }
        }
        loadingTimeStamp = currentNanoTime()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataLayout) return false

        if (fields != other.fields) return false
        if (extra != other.extra) return false
        if (loadingTimeStamp != other.loadingTimeStamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fields.hashCode()
        result = 31 * result + extra.hashCode()
        result = 31 * result + loadingTimeStamp.hashCode()
        return result
    }

    override val name: String get() = fileFilter
}

private val logger = KotlinLogging.logger { }