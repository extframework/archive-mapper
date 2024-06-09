package dev.extframework.archive.mapper

public interface MappingsProvider {
    public val namespaces: Set<String>

    public fun forIdentifier(identifier: String): ArchiveMapping
}