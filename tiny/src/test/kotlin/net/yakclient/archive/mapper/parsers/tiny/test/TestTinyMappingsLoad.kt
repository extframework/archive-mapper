package net.yakclient.archive.mapper.parsers.tiny.test

import net.fabricmc.mappingio.format.tiny.Tiny1FileWriter
import net.yakclient.archive.mapper.*
import net.yakclient.archive.mapper.parsers.tiny.TinyV1MappingsParser
import java.io.FileWriter
import java.net.URL
import kotlin.test.Test

class TestTinyMappingsLoad {
    @Test
    fun `Test basic load`() {
        val `in` = URL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.20.1.tiny").openStream()
        val mappings = TinyV1MappingsParser.parse(`in`)

        Tiny1FileWriter(FileWriter(""))
        val classMapping = mappings.classes[ClassIdentifier("v", MappingType.FAKE)]

        //CLASS	u	net/minecraft/class_6319
        check(classMapping?.realIdentifier?.name == "net/minecraft/class_4239")

        //METHOD v	(Ljava/lang/String;)Ljava/lang/String;	a	method_34675
        check(classMapping?.methods?.get(MethodIdentifier("method_34675", listOf(fromInternalType("Ljava/lang/String;")), MappingType.REAL))?.fakeIdentifier?.name == "a")

        //FIELD	v	Ljava/util/regex/Pattern;	a	field_18956
        check(classMapping?.fields?.get(FieldIdentifier("a", MappingType.FAKE))?.realIdentifier?.name == "field_18956")
    }
}