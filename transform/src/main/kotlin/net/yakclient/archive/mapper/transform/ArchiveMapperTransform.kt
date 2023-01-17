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

        transformMethod { node ->
            mappings.run {
                node.desc = mapMethodDesc(node.desc, direction)

                node.exceptions = node.exceptions.map { mapType(it, direction) }

                node.localVariables.forEach {
                    it.desc = mapType(it.desc, direction)
                }

                node.tryCatchBlocks.forEach {
                    it.type = mapClassName(it.type, direction)
                }

                // AbstractInsnNode
                node.instructions.forEach {
                    when (it) {
                        is FieldInsnNode -> {
                            val mapClassName = mapClassName(it.owner, direction)
                            it.name = run {
                                val mappedClass = getMappedClass(it.owner, direction)
                                    ?.fields
                                    ?.get(
                                        FieldIdentifier(
                                            it.name,
                                            direction.asOppositeType()
                                        )
                                    )

                                when (direction) {
                                    MappingDirection.TO_REAL -> mappedClass?.realIdentifier
                                    MappingDirection.TO_FAKE -> mappedClass?.fakeIdentifier
                                }?.name ?: it.name
                            }
                            it.owner = mapClassName
                            it.desc = mapType(it.desc, direction)
                        }

                        is InvokeDynamicInsnNode -> {
                            // Can ignore name because only the name of the bootstrap method is known at compile time and that is held in the handle field
                            it.desc =
                                mapMethodDesc(it.desc, direction) // Expected descriptor type of the generated call site

                            val desc = mapMethodDesc(it.bsm.desc, direction)
                            it.bsm = Handle(
                                it.bsm.tag,
                                mapType(it.bsm.owner, direction),
                                mapMethodName(it.bsm.owner, it.bsm.name, desc, direction),
                                desc,
                                it.bsm.isInterface
                            )
                        }

                        is MethodInsnNode -> {
                            val mapDesc = mapMethodDesc(it.desc, direction)

                            it.name = mapMethodName(it.owner, it.name, mapDesc, direction)
                            it.owner = mapClassName(it.owner, direction)
                            it.desc = mapDesc
                        }

                        is MultiANewArrayInsnNode -> {
                            it.desc = mapType(it.desc, direction)
                        }

                        is TypeInsnNode -> {
                            it.desc = mapClassName(it.desc, direction)
                        }
                    }
                }
            }

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