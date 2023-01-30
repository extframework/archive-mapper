package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.ArchiveMapping
import net.yakclient.archives.ArchiveReference
import net.yakclient.archives.ArchiveTree
import net.yakclient.archives.Archives
import net.yakclient.archives.transform.AwareClassWriter
import net.yakclient.archives.transform.TransformerConfig
import net.yakclient.common.util.resource.ProvidedResource
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.*
import java.io.InputStream

public fun mappingTransformConfigFor(
    mappings: ArchiveMapping,
    direction: MappingDirection
): TransformerConfig.Mutable {

    // TODO this method is not perfect, it just needs a bit of fine tuning but then it could be really good.
    return TransformerConfig.of {
        transformClass { classNode: ClassNode ->
            mappings.run {
                classNode.fields.forEach { fieldNode ->
                    fieldNode.desc = mapType(fieldNode.desc, direction)
                    fieldNode.name = mapFieldName(classNode.name, fieldNode.name, direction) ?: fieldNode.name
                }

                classNode.methods.forEach { methodNode ->
                    // Mapping other references
                    methodNode.desc = mapMethodDesc(methodNode.desc, direction)

                    methodNode.exceptions = methodNode.exceptions.map { mapType(it, direction) }

                    methodNode.localVariables?.forEach {
                        it.desc = mapType(it.desc, direction)
                    }

                    methodNode.tryCatchBlocks.forEach {
                        if (it.type != null) it.type = mapClassName(it.type, direction) ?: it.type
                    }

                    // AbstractInsnNode
                    methodNode.instructions.forEach { insnNode ->
                        when (insnNode) {
                            is FieldInsnNode -> {
                                insnNode.name = mapFieldName(insnNode.owner, insnNode.name, direction) ?: insnNode.name
                                insnNode.owner = mapClassName(insnNode.owner, direction) ?: insnNode.owner
                                insnNode.desc = mapType(insnNode.desc, direction)
                            }

                            is InvokeDynamicInsnNode -> {
                                // Can ignore name because only the name of the bootstrap method is known at compile time and that is held in the handle field
                                insnNode.desc =
                                    mapMethodDesc(
                                        insnNode.desc,
                                        direction
                                    ) // Expected descriptor type of the generated call site

                                val desc = mapMethodDesc(insnNode.bsm.desc, direction)
                                insnNode.bsm = Handle(
                                    insnNode.bsm.tag,
                                    mapType(insnNode.bsm.owner, direction),
                                    mapMethodName(insnNode.bsm.owner, insnNode.bsm.name, desc, direction)
                                        ?: insnNode.bsm.name,
                                    desc,
                                    insnNode.bsm.isInterface
                                )
                            }

                            is MethodInsnNode -> {
                                val mapDesc = mapMethodDesc(insnNode.desc, direction)

                                insnNode.name = mapMethodName(insnNode.owner, insnNode.name, mapDesc, direction)
                                    ?: (classNode.interfaces + classNode.superName)
                                        .firstNotNullOfOrNull {
                                            mapMethodName(
                                                it,
                                                insnNode.name,
                                                mapDesc,
                                                direction
                                            )
                                        } ?: insnNode.name

                                insnNode.owner = mapClassName(insnNode.owner, direction) ?: insnNode.owner
                                insnNode.desc = mapDesc
                            }

                            is MultiANewArrayInsnNode -> {
                                insnNode.desc = mapType(insnNode.desc, direction)
                            }

                            is TypeInsnNode -> {
                                insnNode.desc = mapClassName(insnNode.desc, direction) ?: insnNode.desc
                            }
                        }
                    }
                }
                classNode.methods.forEach {
                    it.name = mapMethodName(classNode.name, it.name, it.desc, direction)
                        ?: (classNode.interfaces + classNode.superName).filterNotNull().firstNotNullOfOrNull { n ->
                            mapMethodName(n, it.name, it.desc, direction)
                        } ?: it.name
                }

                classNode.name = mapClassName(classNode.name, direction) ?: classNode.name

                classNode.interfaces = classNode.interfaces.map { mapClassName(it, direction) ?: it }

                classNode.superName = mapClassName(classNode.superName, direction) ?: classNode.superName
            }
        }
    }
}



public fun transformClass(
    reader: ClassReader,
    mappings: ArchiveMapping,
    direction: MappingDirection,
    writer: ClassWriter = ClassWriter(Archives.WRITER_FLAGS)
): ByteArray {
    return Archives.resolve(
        reader,
        mappingTransformConfigFor(mappings, direction),
        writer
    )
}

// Archive transforming context
private data class ATContext(
    val theArchive: ArchiveReference,
    val dependencies: List<ArchiveTree>,
    val mappings: ArchiveMapping,
    val config: TransformerConfig,
    val direction: MappingDirection
) {
    fun make() : ClassWriter = MappingAwareClassWriter(
        this
    )
}

private val InputStream.parseNode: ClassNode
    get() {
        val node = ClassNode()
        ClassReader(this).accept(node, 0)
        return node
    }

private fun ArchiveReference.transformAndWriteClass(
    name: String, // Fake location
    context: ATContext
) {
    val realName = context.mappings.mapClassName(name, context.direction)
    if (reader.contains("$realName.class")) return

    val entry = checkNotNull(
        reader["$name.class"]
    ) { "Failed to find class '$name' when transforming archive: '${this.name}'" }


    val resolve = Archives.resolve(
        ClassReader(entry.resource.open()),
        context.config,
        context.make()
    )

    val resource = ProvidedResource(entry.resource.uri) {
        resolve
    }

    val transformedNode = resource.open().parseNode

    val transformedEntry = ArchiveReference.Entry(
        transformedNode.name + ".class",
        resource,
        false,
        this
    )

    writer.remove("$name.class")
    writer.put(transformedEntry)
}

//private fun ArchiveReference.transformOrGet(
//    name: String, // Real name
//    context: ATContext
//): ClassNode? {
//    val resource = getResource("$name.class") ?: run {
//        val fakeClassName =
//            context.mappings.mapClassName(name, MappingDirection.TO_FAKE) ?: return null
//
//        transformAndWriteClass(fakeClassName, context)
//
//        getResource("$name.class")
//    }
//
//    return resource?.parseNode
//}

private class MappingAwareClassWriter(
    private val context: ATContext
) : AwareClassWriter(
    context.dependencies,
    Archives.WRITER_FLAGS,
) {

    private fun getMappedNode(name: String) : HierarchyNode? {
        val node = context.theArchive.reader["$name.class"]?.let {
            val entryInput = it.resource.open()
            val reader = ClassReader(entryInput)
            val node = ClassNode()
            reader.accept(node, 0)

            node
        } ?: run {
            val fakeClassName =
                context.mappings.mapClassName(name, MappingDirection.TO_FAKE) ?: return null

            val entryInput = context.theArchive.reader["$fakeClassName.class"]?.resource?.open() ?: return null
            val reader = ClassReader(entryInput)
            val node = ClassNode()
            reader.accept(node, 0)

            context.config.ct(node)
            node.methods.forEach(context.config.mt)
            node.fields.forEach(context.config.ft)

            node
        }

        return UnloadedClassNode(node)
    }

    override fun loadType(name: String): HierarchyNode {
        return getMappedNode(name) ?: super.loadType(name)
    }
}


public fun transformArchive(
    archive: ArchiveReference,
    dependencies: List<ArchiveTree>,
    mappings: ArchiveMapping,
    direction: MappingDirection,
) {
    val config = mappingTransformConfigFor(mappings, direction)

    val context = ATContext(archive, dependencies, mappings, config, direction)

    archive.reader.entries()
        .filter { it.name.endsWith(".class") }
        .forEach { e ->
            if ((mappings.mapClassName(e.name, MappingDirection.TO_REAL)?.removeSuffix(".class")
                    ?.let { archive.reader.contains(it) } != true)
            )
                archive.transformAndWriteClass(e.name.removeSuffix(".class"), context)
        }
}