package net.yakclient.archive.mapper

public data class TypeIdentifierMapping<T: TypeIdentifier>(
    val real: T,
    val fake: T
)