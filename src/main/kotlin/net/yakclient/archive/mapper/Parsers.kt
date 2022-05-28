package net.yakclient.archive.mapper

import net.yakclient.archive.mapper.parsers.ProGuardMappingParser
import net.yakclient.common.util.ServiceListCollector
import net.yakclient.common.util.ServiceMapCollector

public object Parsers : ServiceMapCollector<String, MappingParser>({ it.name }) {
    init {
        add(ProGuardMappingParser)
    }

    public val PRO_GUARD: String = ProGuardMappingParser.name
}