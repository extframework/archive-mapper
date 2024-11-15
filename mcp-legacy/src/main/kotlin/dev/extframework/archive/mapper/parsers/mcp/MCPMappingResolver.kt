package dev.extframework.archive.mapper.parsers.mcp

import com.durganmcbroom.resources.Resource
import com.durganmcbroom.resources.ResourceAlgorithm
import com.durganmcbroom.resources.VerifiedResource
import com.durganmcbroom.resources.toResource
import dev.extframework.archives.Archives
import dev.extframework.archives.extension.Method
import dev.extframework.common.util.Hex
import dev.extframework.common.util.copyTo
import dev.extframework.common.util.readInputStream
import dev.extframework.common.util.resolve
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

public object MCPMappingResolver {
    // 1 = channel, 2 = mapping version, 3 = mc version
    private const val baseMappingUrl =
        "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_%1\$s/%2\$s-%3\$s/mcp_%1\$s-%2\$s-%3\$s.zip"
    // 1 = mc ver
    private const val newBaseSrgUrl =
        "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/%1\$s/mcp_config-%1\$s.zip"
    // 1 = mc ver
    private const val oldBaseSrgUrl =
        "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/%1\$s/mcp-%1\$s-srg.zip"

    public fun resolve(
        path: Path,

        mcVersion: String,
        channel: String,
        mcpVersion: String,
    ) : Path {
        val outPath = path resolve "rich_mappings.csv"

        if (outPath.exists()) return outPath

        val mappingResource = String.format(baseMappingUrl, channel, mcpVersion, mcVersion).mcpResource()

        val old = mcVersion.startsWith("1.7") || mcVersion.startsWith("1.8") || mcVersion.startsWith("1.9")

        val srgResource = String.format(
            if (old) {
                oldBaseSrgUrl
            } else newBaseSrgUrl, mcVersion
        ).mcpResource()

        val mappingsBasePath = path resolve "mcp_${channel}_${mcpVersion}_$mcVersion"
        unpackIn(
            mappingResource,
            "mcp_${channel}_${mcpVersion}_$mcVersion.zip",
            mappingsBasePath
        )

        val srgBasePath = path resolve "mcp_config-$mcVersion"
        unpackIn(
            srgResource,
            "mcp_config-$mcVersion.zip",
            srgBasePath
        )

        val srgFile = srgBasePath resolve "dist" resolve "joined.srg"
        val staticMethodsPath = srgBasePath resolve "dist" resolve "static_methods.txt"

        val fieldsCsvPath = mappingsBasePath resolve "dist" resolve "fields.csv"
        val methodsCsvPath = mappingsBasePath resolve "dist" resolve "methods.csv"
        val paramsCsvPath = mappingsBasePath resolve "dist" resolve "params.csv"

        data class MemberData(
            val searge: String, val name: String, val comment: String?
        )

        val srgData = SrgInterpreter.parse(srgFile.inputStream())
        val fieldsData = CSVInterpreter.parse(fieldsCsvPath.inputStream(), true) {
            MemberData(it[0], it[1], it.getOrNull(3))
        }.associateBy { it.searge }
        val methodsData = CSVInterpreter.parse(methodsCsvPath.inputStream(), true) {
            MemberData(it[0], it[1], it.getOrNull(3))
        }.associateBy { it.searge }
        val paramsData = CSVInterpreter.parse(paramsCsvPath.inputStream(), true) {
            Pair(it[0], it[1])
        }.associateBy { it.first }
        val staticMethods = staticMethodsPath.toFile().inputStream().bufferedReader().readLines().toSet()

        val richData = srgData.flatMap { data ->
            when (data.type) {
                SrgInterpreter.DataType.PACKAGE -> listOf()
                SrgInterpreter.DataType.CLASS -> listOf(
                    RichMcpMapping(
                        RichMcpMapping.Type.CLASS,
                        data.key,
                        data.value,
                        null
                    )
                )

                SrgInterpreter.DataType.FIELD -> {
                    val className = data.value.substringBeforeLast('/')
                    val rawFieldName = data.value.substringAfterLast('/')

                    val fieldData = fieldsData[rawFieldName]
                    val fieldName = fieldData?.name ?: rawFieldName

                    listOf(
                        RichMcpMapping(
                            RichMcpMapping.Type.FIELD,
                            data.key,
                            "$className/$fieldName",
                            fieldData?.comment?.replace(",", "\$comma\$")
                        )
                    )
                }

                SrgInterpreter.DataType.METHOD -> {
                    val method = Method(data.value.substringBefore(' '))
                    val qualifiedMethodName = method.name
                    val className = qualifiedMethodName.substringBeforeLast('/')
                    val rawMethodName = qualifiedMethodName.substringAfterLast('/')

                    val methodData = methodsData[rawMethodName]
                    val methodName = methodData?.name ?: rawMethodName

                    val methodInfo = RichMcpMapping(
                        RichMcpMapping.Type.METHOD,
                        data.key,
                        "$className/$methodName${method.descriptor}",
                        methodData?.comment?.replace(",", "\$comma\$")
                    )

                    val parameters = if (methodData != null) {
                        val parameterSuffix = "p_${methodData.searge
                            .substringAfter("_")
                            .substringBeforeLast("_")
                        }_"

                        // Unfortunately, MCP decided it was a good idea to base parameter start index on whether it was static or not.
                        // Makes sense for local variables, not so much for parameters -> we have to check if the method is static or not.
                        val startIndex = if (staticMethods.contains(rawMethodName)) 0 else 1

                        val parameterNames = (startIndex until method.argumentTypes.size + startIndex)
                            .map { i ->
                                val name = "$parameterSuffix${i}_"

                                paramsData[name]?.second
                            }

                        parameterNames.withIndex()
                            .filter { it.value != null }
                            .map {
                                RichMcpMapping(
                                    RichMcpMapping.Type.PARAMETER,

                                    "${className}/${methodName}${method.descriptor}_${it.index}",
                                    it.value!!,
                                    null
                                )
                            }
                    } else listOf()

                    listOf(methodInfo) + parameters
                }
            }
        }

        CSVInterpreter.write(
            richData,
            listOf("type", "key", "value", "comment"),
            FileOutputStream(outPath.toFile()),
        ) {
            listOf(it.type.descriptor, it.key, it.value, it.comment)
        }

        return outPath
    }

    // Given a resource, download it and unzip it (assuming it's a zip)
    private fun unpackIn(
        resource: Resource,
        name: String,
        path: Path
    ) {
        val zipPath = path resolve name
        val unpackedPath = path resolve "dist"

        resource copyTo zipPath

        Archives.find(zipPath, Archives.Finders.ZIP_FINDER).use { archive ->
            archive.reader.entries()
                .filterNot { it.isDirectory }
                .forEach { entry ->

                    entry.resource copyTo (unpackedPath resolve entry.name)
                }
        }
    }

    private fun String.mcpResource(): Resource {
        val digestUrl = "$this.sha1"

        val rawResource = URL(this).toResource()

        return VerifiedResource(
            rawResource,
            ResourceAlgorithm.SHA1,
            Hex.parseHex(String(URL(digestUrl).openStream().readInputStream()))
        )
    }
}