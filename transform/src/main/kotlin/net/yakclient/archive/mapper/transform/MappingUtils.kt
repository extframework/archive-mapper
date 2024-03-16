package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.*
import net.yakclient.archives.extension.Method
import net.yakclient.archives.transform.ByteCodeUtils
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter

public fun ArchiveMapping.getMappedClass(jvmName: String, fromNamespace: String): ClassMapping? {
    return classes[ClassIdentifier(
        jvmName, fromNamespace
    )]
}

//public fun ArchiveMapping.mapClassOrType(type: String, fromNamespace: String) : String {
//    return mapClassName(type, fromNamespace)  ?: mapType(type, fromNamespace)
//}

public fun ArchiveMapping.mapClassName(jvmName: String, fromNamespace: String, toNamespace: String): String? {
    val mappedClass = getMappedClass(jvmName, fromNamespace)

    return mappedClass?.getIdentifier(toNamespace)?.name
}

//public fun ArchiveMapping.mapArray(jvmName: String, direction: MappingDirection) : String? {
//    if (jvmName.startsWith("[")) {
//        return  mapArray(jvmName.removePrefix("["), direction)?.let { "[$it" }
//    } else if (jvmName.startsWith("L")) {
//        val trimmedName = jvmName.removePrefix("L").removeSuffix(";")
//        return mapArray(trimmedName, direction)?.let { "L$it;" }
//    } else {
//        return mapClassName(jvmName, direction)
//    }
//}
// All expected to be in jvm class format. ie. org/example/MyClass
// Maps the
public fun ArchiveMapping.mapType(jvmType: String, fromNamespace: String, toNamespace: String): String {
    return if (jvmType.isEmpty()) jvmType
    else if (ByteCodeUtils.primitiveType(jvmType.first()) != null) jvmType
    else if (jvmType.startsWith("[")) {
        "[" + mapType(jvmType.substring(1 until jvmType.length), fromNamespace, toNamespace)
    } else {
        val jvmName = jvmType.removePrefix("L").removeSuffix(";")
        val mapClassName = mapClassName(jvmName, fromNamespace, toNamespace) ?: jvmName
        "L$mapClassName;"
    }
}

public fun ArchiveMapping.mapMethodDesc(desc: String, fromNamespace: String, toNamespace: String): String {
    val signature = Method(desc)
    val parameters = signature.argumentTypes // parameters(signature.desc)

    check(signature.name.isBlank()) { "#mapDesc in 'net.yakclient.components.yak.mapping' is only used to map a method descriptor, not its name and descriptor! use #mapMethodSignature instead!" }

    return parameters.joinToString(
        separator = "",
        prefix = signature.name + "(",
        postfix = ")" + (signature.returnType?.let { mapType(it.descriptor, fromNamespace, toNamespace) } ?: ""),
        transform = {
            mapType(it.descriptor, fromNamespace, toNamespace)
        }
    )
}

public fun ArchiveMapping.mapAnySignature(signature: String, fromNamespace: String, toNamespace: String): String {
    val visitor = object : SignatureWriter() {
        override fun visitClassType(name: String?) {
            super.visitClassType(name?.let { mapClassName(it, fromNamespace, toNamespace) } ?: name)
        }

        override fun visitInnerClassType(name: String?) {
            super.visitInnerClassType(name?.let { mapClassName(it, fromNamespace, toNamespace) } ?: name)
        }
    }
    val reader = SignatureReader(signature)

    reader.accept(visitor)
    return visitor.toString()
}

// Maps a JVM type to a TypeIdentifier
//public fun toTypeIdentifier(type: String): TypeIdentifier = when (type) {
//    "Z" -> BOOLEAN
//    "C" -> CHAR
//    "B" -> BYTE
//    "S" -> SHORT
//    "I" -> INT
//    "F" -> FLOAT
//    "J" -> LONG
//    "D" -> DOUBLE
//    "V" -> VOID
//    else -> {
//        if (type.startsWith("[")) {
//            val type = type.removePrefix("[")
//
//            ArrayTypeIdentifier(toTypeIdentifier(type))
//        } else if (type.startsWith("L") && type.endsWith(";")) ClassTypeIdentifier(
//            type.removePrefix("L").removeSuffix(";")
//        )
//        else throw IllegalArgumentException("Unknown type: '$type' when trying to parse type identifier!")
//    }
//}

public fun ArchiveMapping.mapMethodName(
    cls: String,
    name: String,
    desc: String,
    fromNamespace: String,
    toNamespace: String
): String? {
    val clsMapping = getMappedClass(cls, fromNamespace)

    val method = clsMapping?.methods?.get(
        MethodIdentifier(
            name,
            Method(desc).argumentTypes.toList(),
            fromNamespace
        )
    )

    return method?.getIdentifier(toNamespace)?.name
}

public fun ArchiveMapping.mapFieldName(
    owner: String,
    name: String,
    fromNamespace: String,
    toNamespace: String
): String? {
    val mappedClass = getMappedClass(owner, fromNamespace)
        ?.fields
        ?.get(
            FieldIdentifier(
                name,
                fromNamespace
            )
        )

    return mappedClass?.getIdentifier(toNamespace)?.name
}


//public fun String.withSlashes(): String = replace('.', '/')
//public fun String.withDots(): String = replace('/', '.')