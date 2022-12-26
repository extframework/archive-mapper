package net.yakclient.archive.mapper

public enum class PrimitiveTypeIdentifier(
    _descriptor: Char
) : TypeIdentifier {
    BOOLEAN('Z'),
    CHAR('C'),
    BYTE('B'),
    SHORT('S'),
    INT('I'),
    FLOAT('F'),
    LONG('J'),
    DOUBLE('D'),
    VOID('V');

    override val descriptor: String = _descriptor.toString()
}

public data class ClassTypeIdentifier(
    public val fullQualifier: String,
) : TypeIdentifier {
    public val parts: List<String> = fullQualifier.split('/')
    public val packagePath: List<String> = parts.take(parts.size - 1)
    public val classname: String = parts.last()

    override val descriptor: String = "L$fullQualifier;"
}

public interface WrappedTypeIdentifier : TypeIdentifier {
    public val innerType: TypeIdentifier

    public fun withNew(innerType: TypeIdentifier): WrappedTypeIdentifier // Self
}

public data class ArrayTypeIdentifier(
    override val innerType: TypeIdentifier
) : WrappedTypeIdentifier {
    override val descriptor: String = "[${innerType.descriptor}"

    override fun withNew(innerType: TypeIdentifier): WrappedTypeIdentifier = ArrayTypeIdentifier(innerType)
}