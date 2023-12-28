package net.yakclient.archive.mapper

//public interface MappingContainer {
//    public val namespaces: Set<String>
//}

public interface MappingNodeContainer<T : MappingIdentifier, out N : MappingNode<T>> {
    public val values: Set<N>

    public operator fun get(identifier: T): N?

}

public interface MappingValueContainer<out K> {
    public operator fun get(namespace: String): K?
}

// Impl

public class MappingNodeContainerImpl<T : MappingIdentifier, out N : MappingNode<T>>(
    override val values: Set<N>
) : MappingNodeContainer<T, N> {
    private val namespaceMap: Map<String, Map<T, N>> = values
        .asSequence() // Saves 1 iteration
        .flatMap { n ->
            n.namespaces.map { it to n }
        }.groupBy {
            it.first
        }.mapValues { (k, v) ->
            v.mapNotNull { (_, node) ->
                node.identifiers[k]?.let { it to node }
            }.associate { it }
        }

    override fun get(identifier: T): N? {
        return namespaceMap[identifier.namespace]?.get(identifier)
    }
}

public class MappingValueContainerImpl<out K>(
    private val values: Map<String, K>
) : MappingValueContainer<K> {
    override fun get(namespace: String): K? {
        return values[namespace]
    }

}