package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.*
import net.yakclient.archive.mapper.PrimitiveTypeIdentifier.*
import net.yakclient.archive.mapper.transform.MappingDirection.TO_FAKE
import net.yakclient.archive.mapper.transform.MappingDirection.TO_REAL
import net.yakclient.archives.extension.parameters
import net.yakclient.archives.transform.ByteCodeUtils
import net.yakclient.archives.transform.MethodSignature

public fun ArchiveMapping.getMappedClass(jvmName: String, direction: MappingDirection): ClassMapping? {
    return classes[ClassIdentifier(
        jvmName, direction.asOppositeType()
    )]
}

public fun ArchiveMapping.mapClassName(jvmName: String, direction: MappingDirection): String {
    val mappedClass = getMappedClass(jvmName, direction)

    return when (direction) {
        TO_REAL -> mappedClass?.realIdentifier
        TO_FAKE -> mappedClass?.fakeIdentifier
    }?.name ?: jvmName
}

// All expected to be in jvm class format. ie. org/example/MyClass
// Maps the
public fun ArchiveMapping.mapType(jvmType: String, direction: MappingDirection): String {
    return if (jvmType.isEmpty()) jvmType
    else if (ByteCodeUtils.isPrimitiveType(jvmType.first())) jvmType
    else if (jvmType.startsWith("[")) {
        "[" + mapType(jvmType.substring(1 until jvmType.length), direction)
    } else {
        val mapClassName = mapClassName(jvmType.trim('L', ';'), direction)
        "L$mapClassName;"
    }
}

public fun ArchiveMapping.mapMethodDesc(desc: String, direction: MappingDirection): String {
    val signature = MethodSignature.of(desc)
    val parameters = parameters(signature.desc)

    check(signature.name.isBlank()) { "#mapDesc in 'net.yakclient.components.yak.mapping' is only used to map a method descriptor, not its name and descriptor! use #mapMethodSignature instead!" }

    return parameters.joinToString(
        separator = "",
        prefix = signature.name + "(",
        postfix = ")" + (signature.returnType?.let { mapType(it, direction) } ?: ""),
        transform = {
            mapType(it, direction)
        }
    )
}

public fun ArchiveMapping.mapMethodSignature(cls: String, signature: String, direction: MappingDirection): String {
    val (name, desc, returnType) = MethodSignature.of(signature)
    checkNotNull(returnType) { "Cannot map a method signature with a non-existent return type. Signature was '$signature'" }
    val mappedDesc = mapMethodDesc("($desc)$returnType", direction)
    return mapMethodName(cls, name, signature, direction) + mappedDesc
}

// Maps a JVM type to a TypeIdentifier
public fun toTypeIdentifier(type: String): TypeIdentifier = when (type) {
    "Z" -> BOOLEAN
    "C" -> CHAR
    "B" -> BYTE
    "S" -> SHORT
    "I" -> INT
    "F" -> FLOAT
    "J" -> LONG
    "D" -> DOUBLE
    "V" -> VOID
    else -> {
        if (type.startsWith("[")) {
            val type = type.removePrefix("[")

            ArrayTypeIdentifier(toTypeIdentifier(type))
        } else if (type.startsWith("L") && type.endsWith(";")) ClassTypeIdentifier(
            type.removePrefix("L").removeSuffix(";")
        )
        else throw IllegalArgumentException("Unknown type: '$type' when trying to parse type identifier!")
    }
}

public fun ArchiveMapping.mapMethodName(cls: String, name: String, desc: String, direction: MappingDirection): String {
    val clsMapping = getMappedClass(cls, direction)

    val method = clsMapping?.methods?.get(
        MethodIdentifier(
            name,
            run {
                parameters(MethodSignature.of(desc).desc)
            }.map(::toTypeIdentifier),
            direction.asOppositeType()
        )
    )

    return when (direction) {
        TO_REAL -> method?.realIdentifier
        TO_FAKE -> method?.fakeIdentifier
    }?.name ?: name
}

internal fun MappingDirection.asOppositeType() : MappingType = when (this) {
    TO_REAL -> MappingType.FAKE
    TO_FAKE -> MappingType.REAL
}

//public fun String.withSlashes(): String = replace('.', '/')
//public fun String.withDots(): String = replace('/', '.')