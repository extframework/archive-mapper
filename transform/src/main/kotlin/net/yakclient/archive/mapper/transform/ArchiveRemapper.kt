package net.yakclient.archive.mapper.transform

import org.objectweb.asm.commons.Remapper

// TODO do this eventually, hard with the way they have implemented Invoke Dynamic instructions though
internal class ArchiveRemapper(
    private val context: ArchiveTransformerContext,
) : Remapper() {
    private val srcNamespace: String by context::fromNamespace
    private val dstNamespace: String by context::toNamespace
    private val mappings by context::mappings

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        if (owner.startsWith("[")) return name

        return context.inheritanceTree[owner]?.toCheck()?.firstNotNullOfOrNull {
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
        return context.inheritanceTree[owner]?.toCheck()?.firstNotNullOfOrNull {
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