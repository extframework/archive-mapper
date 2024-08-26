package dev.extframework.archive.mapper.transform

import dev.extframework.archive.mapper.ArchiveMapping
import org.objectweb.asm.commons.Remapper

public class ArchiveRemapper(
    private val mappings: ArchiveMapping,
    private val srcNamespace: String,
    private val dstNamespace: String,
    private val inheritanceTree: ClassInheritanceTree
) : Remapper() {
    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        if (owner.startsWith("[")) return name

        return inheritanceTree[owner]?.toCheck()?.firstNotNullOfOrNull {
            mappings.mapMethodName(
                it,
                name,
                descriptor,
                srcNamespace, dstNamespace
            )
        } ?: name
    }

    override fun mapInvokeDynamicMethodName(name: String?, descriptor: String?): String {
        return super.mapInvokeDynamicMethodName(name, descriptor)
    }

    override fun mapRecordComponentName(
        owner: String, name: String, descriptor: String
    ): String {
        return mappings.mapFieldName(owner, name, srcNamespace, dstNamespace) ?: name
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String?): String {
        return inheritanceTree[owner]?.toCheck()?.firstNotNullOfOrNull {
            mappings.mapFieldName(
                it,
                name,
                srcNamespace,
                dstNamespace
            )
        } ?: name
    }

    override fun map(internalName: String): String {
        return mappings.mapClassName(internalName, srcNamespace, dstNamespace) ?: internalName
    }
}