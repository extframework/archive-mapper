package dev.extframework.archive.mapper.parsers.mcp

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

public object SrgInterpreter {
    public data class LineData(
        val type: DataType,
        val key: String,
        val value: String,
    )

    public enum class DataType {
        PACKAGE,
        CLASS,
        FIELD,
        METHOD,
    }

    public fun parse(data: InputStream): List<LineData> = buildList {
        BufferedReader(InputStreamReader(data)).use { reader ->

            reader.forEachLine { line ->
                val rawType = line.substring(0..1)
                val type = when (rawType) {
                    "PK" -> DataType.PACKAGE
                    "CL" -> DataType.CLASS
                    "MD" -> DataType.METHOD
                    "FD" -> DataType.FIELD
                    else -> throw IllegalArgumentException("Invalid SRG file")
                }

                val lineContent = line.substring(4 until line.length)

                val lineSplit = lineContent.split(' ')
                if (lineSplit.size % 2 != 0) {
                    throw IllegalArgumentException("Invalid line, should be split into even pieces by delimiter ' '")
                }

                val obfuscated = lineSplit.subList(0, lineSplit.size / 2).joinToString("")
                val deobfuscated = lineSplit.subList(lineSplit.size / 2, lineSplit.size).joinToString("")

                add(LineData(type, obfuscated, deobfuscated))
            }
        }
    }
}