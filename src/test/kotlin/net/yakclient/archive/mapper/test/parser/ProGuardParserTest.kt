package net.yakclient.archive.mapper.test.parser

import net.yakclient.archive.mapper.*
import net.yakclient.archive.mapper.parsers.ProGuardMappingParser
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
        val regex = Regex("^(\\d+):(\\d+):(\\S+) (\\S+)\\((\\S+)\\) -> (\\S+)$")

        val input = "144:144:int calculateBufferSize(javax.sound.sampled.AudioFormat,int) -> a"
        assert(regex.matches(input))

        val result = regex.matchEntire(input)!!.groupValues

        assert(result[1] == "144")
        assert(result[2] == "144")
        assert(result[3] == "int")
        assert(result[4] == "calculateBufferSize")
        assert(result[5] == "javax.sound.sampled.AudioFormat,int")
        assert(result[6] == "a")
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
        fun toTypeDescriptor(desc: String): DescriptorType = when (desc) {
            "boolean" -> PrimitiveTypeDescriptor.BOOLEAN
            "char" -> PrimitiveTypeDescriptor.CHAR
            "byte" -> PrimitiveTypeDescriptor.BYTE
            "short" -> PrimitiveTypeDescriptor.SHORT
            "int" -> PrimitiveTypeDescriptor.INT
            "float" -> PrimitiveTypeDescriptor.FLOAT
            "long" -> PrimitiveTypeDescriptor.LONG
            "double" -> PrimitiveTypeDescriptor.DOUBLE
            "void" -> PrimitiveTypeDescriptor.VOID
            else -> {
                if (desc.endsWith("[]")) {
                    val type = desc.removeSuffix("[]")

                    ArrayTypeDescriptor(toTypeDescriptor(type))
                } else ClassTypeDescriptor(desc)
            }
        }

        println(toTypeDescriptor("int[][][]").descriptor)
        println(toTypeDescriptor("java.lang.String[][][]").descriptor)
        println(toTypeDescriptor("long").descriptor)
        println(toTypeDescriptor("net.yakclient.Something").descriptor)
    }

    @Test
    fun `Test Pro Guard Parsing`() {
        val parser = ProGuardMappingParser()

        val mappings = parser.parse(URI("https://launcher.mojang.com/v1/objects/a661c6a55a0600bd391bdbbd6827654c05b2109c/client.txt"))

        println(mappings.classes.size)

        val title = checkNotNull(mappings.classes.getByReal("net.minecraft.client.gui.screens.TitleScreen"))

        val method = title.methods.getByFake("a(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")

        title.methods.size

        println(method?.realName)
    }
}