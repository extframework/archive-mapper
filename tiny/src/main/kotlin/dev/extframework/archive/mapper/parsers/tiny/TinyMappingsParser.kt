package dev.extframework.archive.mapper.parsers.tiny

import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.tree.MappingTree.ElementMapping
import net.fabricmc.mappingio.tree.MemoryMappingTree
import dev.extframework.archive.mapper.*
import dev.extframework.archives.extension.Method
import org.objectweb.asm.Type
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

public interface TinyReader {
    public val version: String

    public fun read(reader: Reader, visitor: MappingVisitor)
}

public open class TinyMappingsParser(
    protected open val reader: TinyReader
) : MappingParser {
    override val name: String = "tiny" + reader.version

    override final fun parse(mappingsIn: InputStream): ArchiveMapping {
        val tree = MemoryMappingTree()
        reader.read(BufferedReader(InputStreamReader(mappingsIn)), tree)
        val namespaces = (tree.dstNamespaces + tree.srcNamespace).filterNotNull().toSet()

//        fun <T : ElementMapping, K : MappingIdentifier> T.toValueContainer(transform: (T, namespace: String) -> K?): MappingValueContainer<K> {
//            return object : MappingValueContainer<K> {
//                override fun get(namespace: String): K? {
//                    return transform(this@toValueContainer, namespace)
//                }
//            }
//        }

        fun <T : ElementMapping, K : MappingIdentifier, N : MappingNode<K>> Collection<T>.toNodeContainer(
            getId: (mapping: T, namespace: String) -> K?,
            getNode: (MappingValueContainer<K>, T) -> N
        ): MappingNodeContainer<K, N> {
            return MappingNodeContainerImpl(
                mapTo(HashSet()) { node ->
                    val ids = namespaces
                        .mapNotNull { namespace -> getId(node, namespace)?.let { node -> namespace to node } }
                        .toMap()

                    getNode(
                        MappingValueContainerImpl(
                            ids,
                        ),
                        node
                    )
                }
            )
        }

        return ArchiveMapping(
            namespaces,
            MappingValueContainerImpl(mapOf()),
            tree.classes.toNodeContainer(getClassId@{ mapping, namespace ->
                ClassIdentifier(
                    mapping.getName(namespace) ?: return@getClassId null,
                    namespace
                )
            }) { cIds, it ->
                ClassMapping(
                    namespaces,
                    cIds,
                    it.methods.toNodeContainer(
                        getMethodId@{ mapping, namespace ->
                            val name = mapping.getName(namespace) ?: return@getMethodId null
                            val desc = mapping.getDesc(namespace) ?: return@getMethodId null

                            MethodIdentifier(
                                name,
                                Method(desc).argumentTypes.toList(),
                                namespace
                            )
                        }
                    ) { mIds, m ->
                        MethodMapping(
                            namespaces,
                            mIds,
                            null, null,
                            MappingValueContainerImpl(
                                namespaces
                                    .mapNotNull { n ->
                                        m.getDesc(n)?.let {
                                            n to it
                                        }
                                    }.associate { (namespace, desc) ->
                                        namespace to Method(desc).returnType!!
                                    }
                            )
                        )
                    },
                    it.fields.toNodeContainer(getFieldId@{ mapping, namespace ->
                        FieldIdentifier(
                            mapping.getName(namespace) ?: return@getFieldId null,
                            namespace
                        )
                    }) { fIds, f ->
                        FieldMapping(
                            namespaces,
                            fIds,
                            MappingValueContainerImpl(
                                namespaces.mapNotNull {
                                    f.getDesc(it)?.let { desc ->
                                        it to desc
                                    }
                                }.associate { (namespace, desc) ->
                                    namespace to Type.getType(desc)
                                })
                        )
                    }
                )
            }
        )
    }
}