package dev.extframework.archive.mapper.parsers.mcp

import dev.extframework.archive.mapper.transform.transformArchive
import dev.extframework.archives.Archives
import dev.extframework.common.util.resolve
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.Path
import kotlin.test.Test

class TestMCPMappingResolver {
    @Test
    fun `Test resolver 1_8_9 mappings`() {
        MCPMappingResolver.resolve(
            Path("test/1.8.9"),
            "1.8.9",
            "stable",
            "22"
        )
    }

    @Test
    fun `Test parse 1_8_9 mappings`() {
        val path = MCPMappingResolver.resolve(
            Path("test/1.8.9"),
            "1.8.9",
            "stable",
            "22"
        )

        val stream = FileInputStream(path.toFile())

        val parser = MCPMappingParser("mojang:obfuscated", "mcp:deobfuscated")

        val mappings = parser.parse(stream)

        println("Finished")
    }

    @Test
    fun `Test remap MC 1_8_9`() {
        val resourceIn = this::class.java.getResource("/1.8.9.jar")!!

        val path = Paths.get(resourceIn.toURI())
        val archive = Archives.find(path, Archives.Finders.ZIP_FINDER)

        fun File.childPaths(): List<Path> {
            return listFiles()?.flatMap { if (it.extension == "jar") listOf(it.toPath()) else it.childPaths() }
                ?: listOf()
        }

        val mappingsPath = MCPMappingResolver.resolve(
            path.parent resolve "mappings" resolve "1.8.9",
            "1.8.9",
            "stable",
            "22"
        )

        val stream = FileInputStream(mappingsPath.toFile())
        val parser = MCPMappingParser("mojang:obfuscated", "mcp:deobfuscated")
        val mappings = parser.parse(stream)

        val childPaths = path.parent.resolve("libraries").toFile().childPaths()
        transformArchive(
            archive,
            childPaths.map { Archives.find(it, Archives.Finders.ZIP_FINDER) },
            mappings,
            "mojang:obfuscated",
            "mcp:deobfuscated",
        )

        val createTempFile = Files.createTempFile(UUID.randomUUID().toString(), ".jar")
        JarOutputStream(FileOutputStream(createTempFile.toFile())).use { target ->
            archive.reader.entries().forEach { e ->
                val entry = JarEntry(e.name)

                target.putNextEntry(entry)

                val eIn = e.open()

                //Stolen from https://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
                val buffer = ByteArray(1024)

                while (true) {
                    val count: Int = eIn.read(buffer)
                    if (count == -1) break

                    target.write(buffer, 0, count)
                }

                target.closeEntry()
            }
        }
        val resolve = Path("src/test/resources/mapped.jar").toAbsolutePath()
        println()
        println(resolve)

        Files.copy(createTempFile, resolve, StandardCopyOption.REPLACE_EXISTING)
    }
}