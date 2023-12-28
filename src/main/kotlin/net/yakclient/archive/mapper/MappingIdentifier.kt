package net.yakclient.archive.mapper

// All the info a mapping node must need to differentiate itself from other same nodes
public interface MappingIdentifier {
    public val name: String
    public val namespace: String
}