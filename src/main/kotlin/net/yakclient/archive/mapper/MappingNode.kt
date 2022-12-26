package net.yakclient.archive.mapper

public interface MappingNode<T: MappingIdentifier> {
    public val realIdentifier: T // The de-obfuscated name
    public val fakeIdentifier: T // Obfuscated name
}