package dev.extframework.archive.mapper.parsers.tiny.test

import dev.extframework.archive.mapper.*
import dev.extframework.archive.mapper.parsers.tiny.TinyV1MappingsParser
import java.net.URL
import kotlin.test.Test

class TestTinyMappingsLoad {
    @Test
    fun `Test basic load`() {
        val `in` = URL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.20.1.tiny").openStream()
        val mappings = TinyV1MappingsParser.parse(`in`)

        val classMapping = mappings.classes[ClassIdentifier("v", "official")]

        //CLASS	u	net/minecraft/class_6319
        check(classMapping?.identifiers?.get("intermediary")?.name == "net/minecraft/class_4239")

        //METHOD v	(Ljava/lang/String;)Ljava/lang/String;	a	method_34675
        check(classMapping?.methods?.get(MethodIdentifier("method_34675", listOf(fromInternalType("Ljava/lang/String;")), "intermediary"))?.getIdentifier("official")?.name == "a")

        //FIELD	v	Ljava/util/regex/Pattern;	a	field_18956
        check(classMapping?.fields?.get(FieldIdentifier("a", "official"))?.getIdentifier("intermediary")?.name == "field_18956")
    }
}