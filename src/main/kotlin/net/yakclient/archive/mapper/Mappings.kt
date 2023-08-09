package net.yakclient.archive.mapper

public data class ArchiveMapping(
    val classes: Map<ClassIdentifier, ClassMapping>,
) : MappingNode<ArchiveIdentifier> {
    override val realIdentifier: ArchiveIdentifier = ArchiveIdentifier.Real
    override val fakeIdentifier: ArchiveIdentifier = ArchiveIdentifier.Fake

    override fun toString(): String {
        return "ArchiveMapping{realIdentifier='$realIdentifier', fakeIdentifier='$fakeIdentifier'}"
    }
}

public sealed class ArchiveIdentifier : MappingIdentifier {
    override val name: String = ""

    public object Real : ArchiveIdentifier() {
        override val type: MappingType = MappingType.REAL
    }

    public object Fake : ArchiveIdentifier() {
        override val type: MappingType = MappingType.FAKE
    }
}

public data class ClassMapping(
    override val realIdentifier: ClassIdentifier,
    override val fakeIdentifier: ClassIdentifier,
    public val methods: Map<MethodIdentifier, MethodMapping>,
    public val fields: Map<FieldIdentifier, FieldMapping>,
) : MappingNode<ClassIdentifier> {
    override fun toString(): String {
        return "ClassMapping{realIdentifier='$realIdentifier', fakeIdentifier='$fakeIdentifier'}"
    }
}

public data class ClassIdentifier(
    override val name: String, override val type: MappingType
) : MappingIdentifier

public data class MethodMapping(
    override val realIdentifier: MethodIdentifier,
    override val fakeIdentifier: MethodIdentifier,
    public val lnStart: Int?,
    public val lnEnd: Int?,
    public val originalLnStart: Int?,
    public val originalLnEnd: Int?,
    public val realReturnType: TypeIdentifier,
    public val fakeReturnType: TypeIdentifier
) : MappingNode<MethodIdentifier>

public data class MethodIdentifier(
    override val name: String, val parameters: List<TypeIdentifier>, override val type: MappingType
) : MappingIdentifier

public data class FieldMapping(
    override val realIdentifier: FieldIdentifier,
    override val fakeIdentifier: FieldIdentifier,
    public val realType: TypeIdentifier,
    public val fakeType: TypeIdentifier
) : MappingNode<FieldIdentifier>

public data class FieldIdentifier(
    override val name: String,
    override val type: MappingType
) : MappingIdentifier