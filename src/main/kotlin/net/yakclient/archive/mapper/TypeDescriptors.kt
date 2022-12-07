package net.yakclient.archive.mapper

public enum class PrimitiveTypeDescriptor(
    _descriptor: Char
) : DescriptorType {
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

public class ClassTypeDescriptor(
    _classname: String
) : DescriptorType {
    public val parts: List<String> = _classname.split('/')
    public val packagePath: List<String> = parts.take(parts.size - 1)
    public val classname: String = parts.last()
    public val fullQualifier: String = _classname

    override val descriptor: String = "L$fullQualifier;"
}

public class ArrayTypeDescriptor(
    public val arrayType: DescriptorType
) : DescriptorType {
    override val descriptor: String = "[${arrayType.descriptor}"
}