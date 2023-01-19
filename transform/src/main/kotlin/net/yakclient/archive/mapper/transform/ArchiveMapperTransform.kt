package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.ArchiveMapping
import net.yakclient.archive.mapper.FieldIdentifier
import net.yakclient.archive.mapper.MappingType
import net.yakclient.archives.ArchiveReference
import net.yakclient.archives.ArchiveTree
import net.yakclient.archives.Archives
import net.yakclient.archives.transform.TransformerConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.*

public fun mappingTransformConfigFor(
    mappings: ArchiveMapping,
    direction: MappingDirection
): TransformerConfig.MutableTransformerConfiguration {
    return TransformerConfig.of {
        transformField { node ->
            node.desc = mappings.mapType(node.desc, direction)

            node
        }

        transformClass { classNode: ClassNode ->
            mappings.run {


                classNode.methods.forEach { methodNode ->
                    // Mapping other references
                    methodNode.desc = mapMethodDesc(methodNode.desc, direction)

                    methodNode.exceptions = methodNode.exceptions.map { mapType(it, direction) }

                    methodNode.localVariables.forEach {
                        it.desc = mapType(it.desc, direction)
                    }

                    methodNode.tryCatchBlocks.forEach {
                        it.type = mapClassName(it.type, direction) ?: it.type
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
                                    ?: (classNode.interfaces + classNode.superName)?.firstNotNullOfOrNull {
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

                classNode.fields.forEach {
                    it.name = mapFieldName(classNode.name, it.name, direction) ?: it.name
                }

                classNode.name = mapClassName(classNode.name, direction) ?: classNode.name

                classNode.interfaces = classNode.interfaces.map { mapClassName(it, direction) ?: it }

            }


            classNode
        }

        transformMethod { node ->


            node
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

public fun transformArchive(
    archive: ArchiveReference,
    mappings: ArchiveMapping,
    direction: MappingDirection,
    dependencies: List<ArchiveTree> = listOf()
) {
    val config = mappingTransformConfigFor(mappings, direction)

    archive.reader.entries()
        .filter { it.name.endsWith(".class") }
        .forEach {
            it.transform(config, dependencies)
        }
}