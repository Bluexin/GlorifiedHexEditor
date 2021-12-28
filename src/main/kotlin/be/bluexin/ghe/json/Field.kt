package be.bluexin.ghe.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes(
    JsonSubTypes.Type(SizeField::class),
    JsonSubTypes.Type(StructureField::class),
    JsonSubTypes.Type(RepeatedSizeField::class),
    JsonSubTypes.Type(RepeatedStructureField::class),
)
sealed interface Field {
    var description: String?
    var offset: Int
    val name: String
}

interface Size {
    val size: Int
}

abstract class Structure(
    structure: String
) : Size {
    val structure: String = structure
        get() = if (::structureRef.isInitialized) structureRef.name else field

    @JsonIgnore
    lateinit var structureRef: LayoutStructure

    @get:JsonIgnore
    override val size: Int
        get() = structureRef.size
}

abstract class Lookup(
    lookup: String
) : Size {
    val lookup: String = lookup
        get() = if (::lookupRef.isInitialized) lookupRef.name else field

    @JsonIgnore
    lateinit var lookupRef: LayoutLookup

    @get:JsonIgnore
    override val size: Int
        get() = lookupRef.size
}

interface Repeated : Field {
    @get:JsonIgnore
    override val name: String
        get() = name("group")
    var repeat: Int
    var element_name: String

    fun name(qualifier: String): String = element_name.replace("\$i", qualifier)
    fun name(index: Int): String = name(index.toString())

    fun offset(index: Int): Int
}

class SizeField(
    override var name: String,
    override var description: String? = null,
    override var offset: Int,
    override var size: Int,
) : Field, Size

class StructureField(
    override var name: String,
    override var description: String?,
    override var offset: Int,
    structure: String
) : Field, Structure(structure)

class LookupField(
    override var name: String,
    override var description: String?,
    override var offset: Int,
    lookup: String
) : Field, Lookup(lookup)

class RepeatedSizeField(
    override var description: String?,
    override var offset: Int,
    @get:JsonInclude
    override var size: Int,
    override var repeat: Int,
    override var element_name: String
) : Field, Size, Repeated {

    override fun offset(index: Int): Int = offset + size * index
}

class RepeatedStructureField(
    override var description: String?,
    override var offset: Int,
    structure: String,
    override var repeat: Int,
    override var element_name: String
) : Field, Structure(structure), Repeated {

    override fun offset(index: Int): Int = offset + size * index
}

class RepeatedLookupField(
    override var description: String?,
    override var offset: Int,
    lookup: String,
    override var repeat: Int,
    override var element_name: String
) : Field, Lookup(lookup), Repeated {

    override fun offset(index: Int): Int = offset + size * index
}
