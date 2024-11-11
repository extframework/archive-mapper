package dev.extframework.archive.mapper.parsers.mcp

import dev.extframework.archive.mapper.*
import dev.extframework.archive.mapper.parsers.mcp.RichMcpMapping.Type.Companion.fromDescriptor
import dev.extframework.archives.extension.Method
import java.io.InputStream

public class MCPMappingParser(
    public val srcNamespace: String,
    public val targetNamespace: String,
) : MappingParser {
    override val name: String = "mcp"

    private val namespaces = setOf(srcNamespace, targetNamespace)

    override fun parse(mappingsIn: InputStream): ArchiveMapping {
        val csvData = CSVInterpreter.parse(mappingsIn, true) {
            RichMcpMapping(fromDescriptor(it[0]), it[1], it[2], it.getOrNull(3))
        }.associateBy { it.key }

        // TODO this whole process could probably happen in 2*O(n), ~3*O(n) is fine but could be reduced
        val types = csvData.values.groupBy { it.type }
        val classes = types[RichMcpMapping.Type.CLASS] ?: listOf()
        val methods = (types[RichMcpMapping.Type.METHOD] ?: listOf()).groupBy {
            Method(it.value).name.substringBeforeLast("/") // Group by class name
        }
        val fields = (types[RichMcpMapping.Type.FIELD] ?: listOf()).groupBy {
            it.value.substringBeforeLast("/") // Group by class name
        }
        val parameters = (types[RichMcpMapping.Type.PARAMETER] ?: listOf()).associateBy {
            it.key // Group by qualified method name
        }

        val classMappings = classes.mapTo(HashSet()) { classData ->
            ClassMapping(
                namespaces,
                MappingValueContainerImpl(
                    mapOf(
                        srcNamespace to ClassIdentifier(classData.key, srcNamespace),
                        targetNamespace to ClassIdentifier(classData.value, targetNamespace)
                    )
                ),
                MappingNodeContainerImpl(
                    methods[classData.value]?.mapTo(HashSet()) { methodData ->
                        val srcMethod = Method(methodData.key)
                        val targetMethod = Method(methodData.value)
                        MethodMapping(
                            namespaces,
                            MappingValueContainerImpl(
                                mapOf(
                                    srcNamespace to MethodIdentifier(
                                        srcMethod.name.substringAfterLast("/"), srcMethod.argumentTypes.toList(), srcNamespace
                                    ),
                                    targetNamespace to MethodIdentifier(
                                        targetMethod.name.substringAfterLast("/"), targetMethod.argumentTypes.toList(), targetNamespace
                                    )
                                )
                            ),
                            null,
                            null,
                            MappingValueContainerImpl(
                                mapOf(
                                    srcNamespace to srcMethod.returnType,
                                    targetNamespace to targetMethod.returnType
                                )
                            ),
                            (0 until targetMethod.argumentTypes.size)
                                .mapNotNull { i ->
                                    val name = "${methodData.value}_$i"

                                    val paramData = parameters[name] ?: return@mapNotNull null

                                    IndexedValue(
                                        i,
                                        MappingValueContainerImpl(
                                            mapOf(
                                                targetNamespace to paramData.value
                                            )
                                        )
                                    )
                                }
                        )
                    } ?: setOf()
                ),
                MappingNodeContainerImpl(
                    fields[classData.value]?.mapTo(HashSet()) { fieldData ->
                        val srcFieldName = fieldData.key.substringAfterLast("/")
                        val targetFieldName = fieldData.value.substringAfterLast("/")

                        FieldMapping(
                            namespaces,
                            MappingValueContainerImpl(
                                mapOf(
                                    srcNamespace to FieldIdentifier(srcFieldName, srcNamespace),
                                    targetNamespace to FieldIdentifier(targetFieldName, targetNamespace),
                                )
                            ),
                            // TODO MCP mappings do not provide field type info?
                            MappingValueContainerImpl(mapOf())
                        )
                    } ?: setOf()
                )
            )
        }

        return ArchiveMapping(
            namespaces,
            MappingValueContainerImpl(mapOf()),
            MappingNodeContainerImpl(
                classMappings
            )
        )
    }
}