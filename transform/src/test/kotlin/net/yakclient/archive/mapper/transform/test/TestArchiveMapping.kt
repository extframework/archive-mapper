package net.yakclient.archive.mapper.transform.test

import net.yakclient.archive.mapper.parsers.proguard.ProGuardMappingParser
import net.yakclient.archive.mapper.parsers.tiny.TinyV1MappingsParser
import net.yakclient.archive.mapper.transform.transformArchive
import net.yakclient.archives.Archives
import net.yakclient.common.util.resolve
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class TestArchiveMapping {
    @Test
    fun `Map Archive`() {
        val resourceIn = this::class.java.getResource("/minecraft-1.20.1.jar")!!
        val mappingsIn = this::class.java.getResourceAsStream("/minecraft-mappings-1.20.1.txt")!!

        val path = Path.of(resourceIn.file)
        val archive = Archives.find(path, Archives.Finders.ZIP_FINDER)

        fun File.childPaths() : List<Path> {
            return listFiles()?.flatMap { if (it.extension == "jar") listOf(it.toPath()) else it.childPaths() } ?: listOf()
        }

        val childPaths = path.parent.resolve("lib").toFile().childPaths()
        transformArchive(
            archive,
            childPaths.map { Archives.find(it, Archives.Finders.ZIP_FINDER) },
            ProGuardMappingParser("official", "named").parse(mappingsIn),
            "official",
            "named",
        )



        val createTempFile = Files.createTempFile(UUID.randomUUID().toString(), ".jar")
        JarOutputStream(FileOutputStream(createTempFile.toFile())).use { target ->
            archive.reader.entries().forEach { e ->
                val entry = JarEntry(e.name)

                target.putNextEntry(entry)

                val eIn = e.resource.open()

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
        val resolve = Files.createTempDirectory("out") resolve "out.jar"
        println()
        println(resolve)

        Files.copy(createTempFile, resolve, StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    fun `Map Archive to intermediary`() {
        val resourceIn = this::class.java.getResource("/minecraft-1.20.1.jar")!!
        val mappingsIn =
            URL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.20.1.tiny").openStream()


        val path = Path.of(resourceIn.file)
        val archive = Archives.find(path, Archives.Finders.ZIP_FINDER)

        fun File.childPaths() : List<Path> {
            return listFiles()?.flatMap { if (it.extension == "jar") listOf(it.toPath()) else it.childPaths() } ?: listOf()
        }

        val childPaths = path.parent.resolve("lib").toFile().childPaths()
        transformArchive(
            archive,
            childPaths.map { Archives.find(it, Archives.Finders.ZIP_FINDER) },
            TinyV1MappingsParser.parse(mappingsIn),
            "official",
            "intermediary",
        )

        val createTempFile = Files.createTempFile(UUID.randomUUID().toString(), ".jar")
        JarOutputStream(FileOutputStream(createTempFile.toFile())).use { target ->
            archive.reader.entries().forEach { e ->
                val entry = JarEntry(e.name)

                target.putNextEntry(entry)

                val eIn = e.resource.open()

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
        val resolve = Files.createTempDirectory("out") resolve "out.jar"
        println()
        println(resolve)

        Files.copy(createTempFile, resolve, StandardCopyOption.REPLACE_EXISTING)
    }

}