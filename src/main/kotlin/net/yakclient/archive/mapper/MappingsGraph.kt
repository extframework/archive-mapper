package net.yakclient.archive.mapper

import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

public interface MappingsGraph {
    public data class ProviderEdge(
        val from: String, val to: String, val provider: MappingsProvider
    )

    public fun connectingEdges(type: String): List<ProviderEdge>
}

public fun newMappingsGraph(list: List<MappingsProvider>): MappingsGraph = object : MappingsGraph {
    // ChatGPT
    private fun generateNamespaceEdges(providers: List<MappingsProvider>): Map<String, List<MappingsGraph.ProviderEdge>> {
        return providers.flatMap { provider ->
            val namespaces = provider.namespaces.toList()
            namespaces.flatMap { from ->
                namespaces.filter { to -> to != from }
                    .map { to -> MappingsGraph.ProviderEdge(from, to, provider) }
            }
        }.groupBy { it.from }
    }

    val outEdges: Map<String, List<MappingsGraph.ProviderEdge>> = generateNamespaceEdges(list)

    override fun connectingEdges(type: String): List<MappingsGraph.ProviderEdge> {
        return outEdges[type] ?: listOf()
    }
}

// BFS
public fun MappingsGraph.findShortest(
    namespaceFrom: String,
    namespaceTo: String,
): MappingsProvider {
    if (namespaceFrom == namespaceTo) return object : MappingsProvider {
        override val namespaces: Set<String> = setOf()

        override fun forIdentifier(identifier: String): ArchiveMapping = ArchiveMapping(
            setOf(),
            MappingValueContainerImpl(mapOf()),
            MappingNodeContainerImpl(setOf()),
        )
    }

    val perimeter: Queue<String> = LinkedList()
    val edgeTo = HashMap<String, MappingsGraph.ProviderEdge>()
    val distTo = HashMap<String, Int>()
    val visited = HashSet<String>()

    perimeter.add(namespaceFrom)
    distTo[namespaceFrom] = 0

    while (!perimeter.isEmpty()) {
        val current = perimeter.remove()
        if (!visited.add(current)) continue

        if (current == namespaceTo) return createProviderFrom(edgeTo, namespaceTo, namespaceFrom)

        for (currentEdge in connectingEdges(current)) {
            perimeter.add(currentEdge.to)

            val currentDist = distTo[currentEdge.to] ?: Int.MAX_VALUE
            val newDist = distTo[currentEdge.from]!! + 1

            if (newDist < currentDist) {
                distTo[currentEdge.to] = newDist
                edgeTo[currentEdge.to] = currentEdge
            }
        }

        visited.add(current)
    }

    throw IllegalStateException("Failed to find path between mappings")
}

private fun createProviderFrom(
    edges: Map<String, MappingsGraph.ProviderEdge>, typeTo: String, typeFrom: String
): MappingsProvider {
    fun createPath(vertex: String): List<MappingsGraph.ProviderEdge> {
        val edge = edges[vertex] ?: return listOf()
        return createPath(edge.from) + edge
    }

    val edgePath = createPath(typeTo)
    return object : MappingsProvider {
        override val namespaces: Set<String> = setOf(typeFrom, typeTo)

        override fun forIdentifier(identifier: String): ArchiveMapping {
            // Super expensive depending on length of path*
            val path = edgePath.map {
                DirectedMappingNode(
                    DirectedMappingType(it.from), DirectedMappingType(it.to),
                    it.provider.forIdentifier(identifier)
                )
            }

            return joinMappings(path)
        }
    }
}

public class DirectedMappingType internal constructor(
    public val namespace: String
) {
    // Attempt to get an identifier matching the current namespace, if it cant be found, go through the other namespaces
    // and look for a match (the only real reason we do this that there may be a mapping that does not change across namespaces
    // and if that is the case, it may not be present in other namespaces so we just keep looking -- I dont really like
    // this, so this may change in future versions of archive-mapper)
    public fun <T : MappingIdentifier> get(mappingNode: MappingNode<T>): T =
        mappingNode.getIdentifier(namespace) ?: mappingNode.namespaces
            .toMutableList().apply { remove(namespace) }
            .firstNotNullOf {
                mappingNode.getIdentifier(it)
            }
}

public data class DirectedMappingNode<T : MappingNode<*>>(
    val from: DirectedMappingType, val to: DirectedMappingType, val node: T
)

// Included out here for debugging
private fun <K, V> Collection<V>.doublyAssociateBy(
    keySelector: (V) -> Pair<K, K>
): Map<K, V> {
    return associateBy { keySelector(it).first } + associateBy { keySelector(it).second }
}

private fun <T, R, K> List<T>.foldingMap(
    initial: R, mapper: (R, T) -> Pair<K, R>
): Pair<List<K>, R> {
    var accumulator = initial

    return map {
        val pair = mapper(accumulator, it)
        accumulator = pair.second
        pair.first
    } to accumulator
}


// We assume that the order given to us is from fake to real, thats how this archive will be built.
public fun joinMappings(path: List<DirectedMappingNode<ArchiveMapping>>): ArchiveMapping {
    if (path.size == 1) return path.first().node
    val fromNS = path.first().from.namespace
    val toNS = path.last().to.namespace
    val namespaces = setOf(fromNS, toNS)
    return ArchiveMapping(
        namespaces,
        MappingValueContainerImpl(namespaces.associateWith { n ->
            ArchiveIdentifier("", n)
        }),
        MappingNodeContainerImpl(path.first().node.classes.values
            .asSequence()
            .map { path.first().from.get(it) }
            .mapTo(HashSet()) { fromClassIdentifier: ClassIdentifier ->
                val (classes, toClassIdentifier: ClassIdentifier) = path.foldingMap(fromClassIdentifier) { acc, it ->
                    val node = it.node.classes[acc]
                        ?: throw IllegalStateException("Failed to follow reference chain from ${fromClassIdentifier.namespace} class: '${fromClassIdentifier.name}' to appropriate remapping.")

                    DirectedMappingNode(
                        it.from, it.to, node
                    ) to it.to.get(node)
                }

                ClassMapping(
                    namespaces,
                    MappingValueContainerImpl(
                        mapOf(
                            fromNS to fromClassIdentifier,
                            toNS to toClassIdentifier,
                        )
                    ),
                    MappingNodeContainerImpl(classes.first().node.methods.values.map { classes.first().from.get(it) }
                        .mapTo(HashSet()) { fromMethodIdentifier ->
                            val (methods, toMethodIdentifier) = classes.foldingMap(fromMethodIdentifier) { acc, it ->
                                val methodNode = it.node.methods[acc]
                                    ?: throw IllegalStateException("Failed to follow reference chain from ${fromMethodIdentifier.namespace} method: '${fromMethodIdentifier.name}' to appropriate remapping. In ${fromClassIdentifier.namespace} class '${fromClassIdentifier.name}'")

                                DirectedMappingNode(
                                    it.from, it.to, methodNode
                                ) to it.to.get(methodNode)
                            }

                            // By default fake is the first, real is the last
                            val (toRT, fromRT) = (run {
                                val directed = methods.last()
                                directed.node.returnType[directed.to.namespace]!!
                            } to run {
                                val directed = methods.first()
                                directed.node.returnType[directed.from.namespace]!!
                            })

                            MethodMapping(
                                namespaces,
                                MappingValueContainerImpl(
                                    mapOf(
                                        toNS to toMethodIdentifier,
                                        fromNS to fromMethodIdentifier,
                                    )
                                ),
                                methods.first().node.lnStart,
                                methods.first().node.lnEnd,
                                MappingValueContainerImpl(
                                    mapOf(
                                        toNS to toRT, fromNS to fromRT
                                    )
                                )
                            )
                        }),
                    MappingNodeContainerImpl(classes.first().node.fields.values.map { classes.first().from.get(it) }
                        .mapTo(HashSet()) { fromFieldIdentifier ->
                            val (fields, toFieldIdentifier) = classes.foldingMap(fromFieldIdentifier) { acc, it ->
                                val fieldNode = it.node.fields[acc]
                                    ?: throw IllegalStateException("Failed to follow reference chain from ${fromFieldIdentifier.namespace} field: '${fromFieldIdentifier.name}' to appropriate remapping. In ${fromClassIdentifier.namespace} class '${fromClassIdentifier.name}'")

                                DirectedMappingNode(
                                    it.from, it.to, fieldNode
                                ) to it.to.get(fieldNode)
                            }

                            val (toType, fromType) = run {
                                val directed = fields.last()

                                directed.node.type[directed.to.namespace]!!

                            } to run {
                                val directed = fields.first()

                                directed.node.type[directed.from.namespace]!!
                            }

                            FieldMapping(
                                namespaces,
                                MappingValueContainerImpl(
                                    mapOf(
                                        toNS to toFieldIdentifier,
                                        fromNS to fromFieldIdentifier,
                                    )
                                ),
                                MappingValueContainerImpl(
                                    mapOf(
                                        toNS to toType,
                                        fromNS to fromType
                                    )
                                ),
                            )
                        })
                )
            })
    )
}