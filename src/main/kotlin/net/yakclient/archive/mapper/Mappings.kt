package net.yakclient.archive.mapper

public data class MappedArchive(
    override val realName: String, override val fakeName: String,
    val classes: ObfuscationMap<MappedClass>
) : MappedNode

public data class MappedClass(
    override val realName: String, override val fakeName: String,
    public val methods: ObfuscationMap<MappedMethod>,
    public val fields: ObfuscationMap<MappedField>
) : MappedNode

public data class MappedMethod(
    override val realName: String,
    override val fakeName: String,
    public val lnStart: Int?,
    public val lnEnd: Int?,
    public val originalLnStart: Int?,
    public val originalLnEnd: Int?,
    public val parameters: List<DescriptorType>,
    public val returnType: DescriptorType
) : MappedNode

public data class MappedField(
    override val realName: String,
    override val fakeName: String,
    public val type: DescriptorType
) : MappedNode