package net.yakclient.archive.mapper.parsers

import net.yakclient.archive.mapper.MappingParser
import net.yakclient.archive.mapper.ParserProvider

internal class DefaultParserProvider : ParserProvider {
    override fun provide(name: String): MappingParser? = when(name) {
        PRO_GUARD_PARSER_NAME -> ProGuardMappingParser()
        else -> null
    }
}