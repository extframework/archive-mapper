package dev.extframework.archive.mapper.parsers.tiny.test

import net.fabricmc.mappingio.format.tiny.Tiny1FileWriter
import dev.extframework.archive.mapper.parsers.tiny.TinyV1MappingsParser
import dev.extframework.archive.mapper.parsers.tiny.write
import dev.extframework.common.util.resolve
import org.junit.jupiter.api.Test
import java.io.FileWriter
import java.net.URL
import java.nio.file.Files
import kotlin.io.path.inputStream

class TestMappingWrite {
    @Test
    fun `Load then write mappings`() {
        val `in` =
            URL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.20.1.tiny").openStream()
        val mappings = TinyV1MappingsParser.parse(`in`)

        val dir = Files.createTempDirectory("tiny-out")
        write(Tiny1FileWriter(FileWriter((dir resolve "out.tiny").toFile())), mappings,
            "official",
            "intermediary")
        println(dir)

        TinyV1MappingsParser.parse((dir resolve "out.tiny").inputStream())
    }
}