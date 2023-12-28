package net.yakclient.archive.mapper.parsers.proguard.test

import net.yakclient.archive.mapper.*
import net.yakclient.archive.mapper.parsers.proguard.ProGuardMappingParser
import java.net.URI
import kotlin.test.Test


class ProGuardParserTest {
    @Test
    fun `Test class regex`() {
        val regex = Regex("""^(\S+) -> (\S+):$""")

        val input = "com.mojang.blaze3d.audio.Channel -> drj:"

        assert(regex.matches(input))

        val result = regex.matchEntire(input)!!.groupValues

        assert(result[1] == "com.mojang.blaze3d.audio.Channel")
        assert(result[2] == "drj")
    }

    @Test
    fun `Test Method regex`() {
        val regex =
            Regex("""^((?<from>\d+):(?<to>\d+):)?(?<ret>[^:]+)\s(?<name>[^:]+)\((?<args>.*)\)((:(?<originalFrom>\d+))?(:(?<originalTo>\d+))?)?\s->\s(?<obf>[^:]+)""")

        val input = "144:144:int calculateBufferSize(javax.sound.sampled.AudioFormat,int) -> a"
        assert(regex.matches(input))

        val result = regex.matchEntire(input)!!.groups as MatchNamedGroupCollection

        assert(result["from"]!!.value == "144")
        assert(result["to"]!!.value == "144")
        assert(result["ret"]!!.value == "int")
        assert(result["name"]!!.value == "calculateBufferSize")
        assert(result["args"]!!.value == "javax.sound.sampled.AudioFormat,int")
        assert(result["obf"]!!.value == "a")
    }

    @Test
    fun `Test Field Regex`() {
        val regex = Regex("""^(\S+) (\S+) -> (\S+)$""")

        val input = "boolean supportsDisconnections -> f"

        assert(regex.matches(input))

        val result = regex.matchEntire(input)!!.groupValues

        assert(result[1] == "boolean")
        assert(result[2] == "supportsDisconnections")
        assert(result[3] == "f")
    }

    @Test
    fun `Test type descriptor conversion`() {
        fun toTypeDescriptor(desc: String): TypeIdentifier = when (desc) {
            "boolean" -> PrimitiveTypeIdentifier.BOOLEAN
            "char" -> PrimitiveTypeIdentifier.CHAR
            "byte" -> PrimitiveTypeIdentifier.BYTE
            "short" -> PrimitiveTypeIdentifier.SHORT
            "int" -> PrimitiveTypeIdentifier.INT
            "float" -> PrimitiveTypeIdentifier.FLOAT
            "long" -> PrimitiveTypeIdentifier.LONG
            "double" -> PrimitiveTypeIdentifier.DOUBLE
            "void" -> PrimitiveTypeIdentifier.VOID
            else -> {
                if (desc.endsWith("[]")) {
                    val type = desc.removeSuffix("[]")

                    ArrayTypeIdentifier(toTypeDescriptor(type))
                } else ClassTypeIdentifier(desc)
            }
        }

        println(toTypeDescriptor("int[][][]").descriptor)
        println(toTypeDescriptor("java.lang.String[][][]").descriptor)
        println(toTypeDescriptor("long").descriptor)
        println(toTypeDescriptor("net.yakclient.Something").descriptor)
    }

    @Test
    fun `Test Pro Guard Parsing`() {
        val parser = ProGuardMappingParser(
            "official", "named"
        )

        val mappings = parser.parse(
            URI("https://launcher.mojang.com/v1/objects/a661c6a55a0600bd391bdbbd6827654c05b2109c/client.txt").toURL()
                .openStream()
        )

        println(mappings.classes.values.size)

        val title = checkNotNull(
            mappings.classes.get(ClassIdentifier(
                "net/minecraft/client/gui/screens/TitleScreen",
                "named"
            )
        ))

        val method = title.methods.get(MethodIdentifier(
            "a",
            listOf(
                ClassTypeIdentifier("dtm"),
                PrimitiveTypeIdentifier.INT,
                PrimitiveTypeIdentifier.INT,
                PrimitiveTypeIdentifier.FLOAT
            ),
            "official"
        ))


        println(method?.identifiers?.get("named")?.name)
    }
}