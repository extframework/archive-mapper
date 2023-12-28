package net.yakclient.archive.mapper

public data class ArchiveMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< ArchiveIdentifier>,

    val classes: MappingNodeContainer<ClassIdentifier, ClassMapping>,
) : MappingNode<ArchiveIdentifier>

public data class ArchiveIdentifier(
    override val name: String, override val namespace: String

) : MappingIdentifier {
//    override val name: String = ""
//
//    public data object Real : ArchiveIdentifier() {
//        override val type: String = MappingType.REAL
//    }
//
//    public data object Fake : ArchiveIdentifier() {
//        override val type: MappingType = MappingType.FAKE
//    }
}

public data class ClassMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< ClassIdentifier>,

    public val methods: MappingNodeContainer<MethodIdentifier, MethodMapping>,
    public val fields: MappingNodeContainer<FieldIdentifier, FieldMapping>,
) : MappingNode<ClassIdentifier>

public data class ClassIdentifier(
    override val name: String, override val namespace: String
) : MappingIdentifier

public data class MethodMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< MethodIdentifier>,

    public val lnStart: MappingValueContainer<Int>?,
    public val lnEnd: MappingValueContainer<Int>?,

    public val returnType: MappingValueContainer<TypeIdentifier>,
) : MappingNode<MethodIdentifier>

public data class MethodIdentifier(
    override val name: String, val parameters: List<TypeIdentifier>, override val namespace: String
) : MappingIdentifier

public data class FieldMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< FieldIdentifier>,

    public val type: MappingValueContainer<TypeIdentifier>
) : MappingNode<FieldIdentifier>

public data class FieldIdentifier(
    override val name: String,
    override val namespace: String
) : MappingIdentifier