package net.yakclient.archive.mapper.parsers.tiny

import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader
import java.io.Reader

public object TinyV1Reader : TinyReader {
    override fun read(reader: Reader, visitor: MappingVisitor) {
        Tiny1FileReader.read(reader, visitor)
    }
}

public object TinyV2Reader : TinyReader {
    override fun read(reader: Reader, visitor: MappingVisitor) {
        Tiny2FileReader.read(reader, visitor)
    }
}

public object TinyV1MappingsParser : TinyMappingsParser(TinyV1Reader)

public object TinyV2MappingsParser : TinyMappingsParser(TinyV2Reader)