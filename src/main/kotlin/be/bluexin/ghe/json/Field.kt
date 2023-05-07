package be.bluexin.ghe.json

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
sealed interface Field {
    val description: String?
    val offset: Int
    val name: String

    @get:JsonIgnore
    val enabled: Boolean get() = true
}

open class Size(
    override val name: String,
    override val description: String? = null,
    override val offset: Int,
    open val size: Int,
) : Field {

    override fun toString(): String {
        return "Size(name='$name', description=$description, offset=$offset, size=$size)"
    }
}

open class Structure(
    name: String,
    description: String?,
    offset: Int,
    structure: String
) : Size(name, description, offset, 0), Field {
    val structure: String = structure
        get() = if (enabled) structureRef.name else field

    @JsonIgnore
    lateinit var structureRef: LayoutStructure

    @get:JsonIgnore
    override val size: Int
        get() = if (enabled) structureRef.size else 0

    override val enabled: Boolean
        get() = ::structureRef.isInitialized
}

open class Lookup(
    name: String,
    description: String?,
    offset: Int,
    lookup: String
) : Size(name, description, offset, 0), Field {
    val lookup: String = lookup
        get() = if (enabled) lookupRef.name else field

    @JsonIgnore
    lateinit var lookupRef: LayoutLookup

    @get:JsonIgnore
    override val size: Int
        get() = if (enabled) lookupRef.size else 0

    override val enabled: Boolean
        get() = ::lookupRef.isInitialized
}

interface Repeated : Field {
    @get:JsonIgnore
    override val name: String
        get() = name("group")
    val repeat: Int

    @get:JsonProperty("element_name")
    val elementName: String

    fun name(qualifier: String): String = elementName.replace("\$i", qualifier)
    fun name(index: Int): String = name(index.toString())

    fun offset(index: Int): Int
}

class RepeatedSizeField(
    override var description: String?,
    override var offset: Int,
    @get:JsonInclude
    override var size: Int,
    override var repeat: Int,
    override var elementName: String
) : Field, Size("", description, offset, 0), Repeated {
    override val name: String
        get() = super<Repeated>.name

    override fun offset(index: Int): Int = offset + size * index
}

class RepeatedStructureField(
    description: String?,
    offset: Int,
    structure: String,
    override var repeat: Int,
    override var elementName: String
) : Field, Structure("", description, offset, structure), Repeated {
    override val name: String
        get() = super<Repeated>.name

    override fun offset(index: Int): Int = offset + size * index
}

class RepeatedLookupField(
    description: String?,
    offset: Int,
    lookup: String,
    override var repeat: Int,
    override var elementName: String
) : Lookup("", description, offset, lookup), Repeated {
    override val name: String
        get() = super<Repeated>.name

    override fun offset(index: Int): Int = offset + size * index
}
