package net.yakclient.archive.mapper

import net.yakclient.archive.mapper.parsers.PRO_GUARD_PARSER_NAME
import java.util.*

public object Parsers {
    private val parsers = LazyMap { name: String ->
        ServiceLoader.load(ParserProvider::class.java).firstNotNullOfOrNull { it.provide(name) }
    }

    public const val PRO_GUARD: String = PRO_GUARD_PARSER_NAME

    public operator fun get(name: String) : MappingParser? = parsers[name]

    private class LazyMap<K, out V>(
        private val delegate: MutableMap<K, V> = HashMap(),
        private val lazyImpl : (K) -> V?
    ) : Map<K, V> by delegate {
        override fun get(key: K): V? {
            if (delegate.contains(key)) return delegate[key]
            val lazyVal = lazyImpl(key)
            if (lazyVal != null) delegate[key] = lazyVal
            return lazyVal
        }
    }
}