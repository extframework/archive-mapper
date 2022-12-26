package net.yakclient.archive.mapper

import java.io.InputStream

public interface MappingParser {
    public val name: String

    public fun parse(mappingsIn: InputStream) : ArchiveMapping
}