package net.yakclient.archive.mapper

public class ObfuscationMap<V : MappedNode>(
    private val delegate: Map<Pair<String, String>, V> = HashMap()
) : Map<Pair<String, String>, V> by delegate {

    public constructor(nodes: List<V>) : this(nodes.associateBy { it.realName to it.fakeName })

    private val realNames = delegate.mapKeys { it.key.first }
    private val fakeNames = delegate.mapKeys { it.key.second }

    public fun getByReal(name: String): V? = realNames[name]
    public fun getByFake(name: String): V? = fakeNames[name]

    public fun containsReal(name: String) : Boolean = realNames.containsKey(name)
    public fun containsFake(name: String) : Boolean = fakeNames.containsKey(name)
}