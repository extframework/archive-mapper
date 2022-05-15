package net.yakclient.archive.mapper

public interface ParserProvider {
    public fun provide(name: String) : MappingParser?
}