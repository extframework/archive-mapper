package net.yakclient.archive.mapper

import java.io.InputStream
import java.net.URI
import java.net.URL

public interface MappingParser {
    public val name: String

    public fun parse(mappingsIn: InputStream) : MappedArchive

    public fun parse(mappings: URL) : MappedArchive = parse(mappings.openStream())

    public fun parse(mappings: URI): MappedArchive = parse(mappings.toURL())
}