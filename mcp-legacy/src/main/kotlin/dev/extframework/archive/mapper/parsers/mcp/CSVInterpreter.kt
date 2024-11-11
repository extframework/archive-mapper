package dev.extframework.archive.mapper.parsers.mcp

import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream

public object CSVInterpreter {
    public fun parse(
        stream: InputStream,
        hasHeaders: Boolean,
    ): List<List<String>> = parse(stream, hasHeaders) { it }

    public fun <T> parse(
        stream: InputStream,
        hasHeaders: Boolean,
        constructor: (List<String>) -> T
    ): List<T> = buildList {
        stream.bufferedReader().use { reader ->
            reader.useLines { lines ->
                val iterator = lines.iterator()
                if (hasHeaders) iterator.next()

                iterator.forEach { line ->
                    add(constructor(line.split(",")))
                }
            }
        }
    }

    public fun <T> write(
        data: List<T>,
        headers: List<String>?,

        stream: OutputStream,
        deConstructor: (T) -> List<String?>
    ): Unit = stream.bufferedWriter().use { writer ->
        if (headers != null) {
            writer.writeLine(headers.joinToString(separator = ","))
        }

        data.forEach { line ->
            val list: List<String?> = deConstructor(line)

            var nulled = false

            list.forEach {
                if (it == null) {
                    nulled = true
                } else if (nulled) {
                    throw IllegalArgumentException("Given data is invalid, only values at the end of the list can be null")
                }
            }

            writer.writeLine(list.filterNotNull().joinToString(separator = ","))
        }
    }

    private fun BufferedWriter.writeLine(line: String) {
        write(line)
        write("\n")
    }
}