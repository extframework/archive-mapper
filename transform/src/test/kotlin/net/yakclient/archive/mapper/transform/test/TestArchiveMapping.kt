package net.yakclient.archive.mapper.transform.test

import net.yakclient.archive.mapper.parsers.ProGuardMappingParser
import net.yakclient.archive.mapper.transform.MappingDirection
import net.yakclient.archive.mapper.transform.transformArchive
import net.yakclient.archives.Archives
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class TestArchiveMapping {
    @Test
    fun `Map Archive`() {
        val resourceIn = this::class.java.getResource("/minecraft-1.18.jar")
        val mappingsIn = this::class.java.getResourceAsStream("/minecraft-mappings-1.18.txt")

        val path = Path.of(resourceIn.file)
        val archive = Archives.find(path, Archives.Finders.ZIP_FINDER)

        fun File.childPaths() : List<Path> {
            return listFiles()?.flatMap { if (it.extension == "jar") listOf(it.toPath()) else it.childPaths() } ?: listOf()
        }

        val childPaths = Path.of(this::class.java.getResource("/minecraft-1.18.jar").file).parent.resolve("lib").toFile().childPaths()
        println(childPaths)
        transformArchive(
            archive,
            childPaths.map { Archives.find(it, Archives.Finders.ZIP_FINDER) },
            ProGuardMappingParser.parse(mappingsIn),
            direction = MappingDirection.TO_REAL,
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
        val resolve = Path.of(resourceIn.path).parent.resolve("out.jar")
        println(resolve)

        Files.copy(createTempFile, resolve, StandardCopyOption.REPLACE_EXISTING)
    }
}