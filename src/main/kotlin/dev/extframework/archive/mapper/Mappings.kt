package dev.extframework.archive.mapper

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method

public data class ArchiveMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< ArchiveIdentifier>,

    val classes: MappingNodeContainer<ClassIdentifier, ClassMapping>,
) : MappingNode<ArchiveIdentifier>

public data class ArchiveIdentifier(
    override val name: String, override val namespace: String
) : MappingIdentifier

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
    override val identifiers: MappingValueContainer<MethodIdentifier>,

    public val lnStart: MappingValueContainer<Int>?,
    public val lnEnd: MappingValueContainer<Int>?,

    public val returnType: MappingValueContainer<Type>,
    // Parameter indices are 0 based despite `this` or static methods. Parameter 0 will always be the
    // first parameter as shows up naturally in java.
    public val parameterNames: List<IndexedValue<MappingValueContainer<String>>> = ArrayList()
) : MappingNode<MethodIdentifier>

public data class MethodIdentifier(
    override val name: String, val parameters: List<Type>, override val namespace: String
) : MappingIdentifier {
    public constructor(method: Method, namespace: String) : this(method.name, method.argumentTypes.toList(), namespace)
}

public data class FieldMapping(
    override val namespaces: Set<String>,
    override val identifiers: MappingValueContainer< FieldIdentifier>,

    public val type: MappingValueContainer<Type>
) : MappingNode<FieldIdentifier>

public data class FieldIdentifier(
    override val name: String,
    override val namespace: String
) : MappingIdentifier