package net.yakclient.archive.mapper

public interface MappedNode {
    public val realName: String // The de-obfuscated name
    public val fakeName: String // Obfuscated name
}