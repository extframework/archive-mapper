package net.yakclient.archive.mapper

public interface MappingNode<T: MappingIdentifier> {
    public val namespaces: Set<String>
    public val identifiers: MappingValueContainer<T>

    public fun getIdentifier(namespace: String) : T? {
        return identifiers[namespace]
    }

//    public val realIdentifier: T // The de-obfuscated name
//    public val fakeIdentifier: T // Obfuscated name
}