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
    public val classname: String
) : DescriptorType {
    override val descriptor: String = "L${classname.replace('.', '/')};"
}

public class ArrayTypeDescriptor(
    public val arrayType: DescriptorType
) : DescriptorType {

    override val descriptor: String = "[${arrayType.descriptor}"
}