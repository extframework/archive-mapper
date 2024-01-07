package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.*
import net.yakclient.archives.ArchiveReference
import net.yakclient.archives.ArchiveTree
import net.yakclient.archives.Archives
import net.yakclient.archives.transform.AwareClassWriter
import net.yakclient.archives.transform.TransformerConfig
import net.yakclient.common.util.resource.ProvidedResource
import org.objectweb.asm.*
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.tree.*
import java.io.InputStream

internal fun ClassInheritancePath.toCheck(): List<String> {
    return listOf(name) + interfaces.flatMap { it.toCheck() } + (superClass?.toCheck() ?: listOf())
}

public fun mappingTransformConfigFor(
    context: ArchiveTransformerContext,
): TransformerConfig.Mutable {
    val remapper = ArchiveRemapper(context)
    return TransformerConfig.of {
        transformClass {
            val newNode = ClassNode()
            val classRemapper = ClassRemapper(newNode, remapper)
            it.accept(classRemapper)

            newNode
        }
    }

//    return TransformerConfig.of {
//        transformClass { classNode: ClassNode ->
//            mappings.run {
//                classNode.fields.forEach { fieldNode ->
//                    fieldNode.desc = mapType(fieldNode.desc, fromNamespace, toNamespace)
//                    fieldNode.name =
//                        mapFieldName(classNode.name, fieldNode.name, fromNamespace, toNamespace) ?: fieldNode.name
//                    if (fieldNode.signature != null)
//                        fieldNode.signature = mapAnySignature(fieldNode.signature, fromNamespace, toNamespace)
//                }
//
//                classNode.methods.forEach { methodNode ->
//                    // Mapping other references
//                    methodNode.name =
//                        mapMethodName(classNode.name, methodNode.name, methodNode.desc, fromNamespace, toNamespace)
//                            ?: (classNode.interfaces + classNode.superName).filterNotNull().firstNotNullOfOrNull { n ->
//                                mapMethodName(n, methodNode.name, methodNode.desc, fromNamespace, toNamespace)
//                            } ?: methodNode.name
//
//                    methodNode.desc = mapMethodDesc(methodNode.desc, fromNamespace, toNamespace)
//
//                    if (methodNode.signature != null)
//                        methodNode.signature = mapAnySignature(methodNode.signature, fromNamespace, toNamespace)
//
//                    methodNode.exceptions = methodNode.exceptions.map {
//                        mapClassName(it, fromNamespace, toNamespace) ?: it
//                    }
//
//                    methodNode.localVariables?.forEach {
//                        it.desc = mapType(it.desc, fromNamespace, toNamespace)
//                        if (it.signature != null) it.signature =
//                            mapAnySignature(it.signature, fromNamespace, toNamespace)
//                    }
//
//                    methodNode.tryCatchBlocks.forEach {
//                        if (it.type != null) it.type = mapClassName(it.type, fromNamespace, toNamespace) ?: it.type
//                    }
//
//                    // AbstractInsnNode
//                    methodNode.instructions.forEach { insnNode ->
//                        when (insnNode) {
//                            is FieldInsnNode -> {
//                                insnNode.name = tree[insnNode.owner]?.toCheck()?.firstNotNullOfOrNull {
//                                    mapFieldName(
//                                        it,
//                                        insnNode.name,
//                                        fromNamespace,
//                                        toNamespace
//                                    )
//                                } ?: insnNode.name
//
//                                insnNode.owner =
//                                    mapClassName(insnNode.owner, fromNamespace, toNamespace) ?: insnNode.owner
//                                insnNode.desc = mapType(insnNode.desc, fromNamespace, toNamespace)
//                            }
//
//                            is InvokeDynamicInsnNode -> {
//                                fun Handle.mapHandle(): Handle {
//                                    return if (
//                                        tag.equalsAny(
//                                            Opcodes.H_INVOKEVIRTUAL,
//                                            Opcodes.H_INVOKESTATIC,
//                                            Opcodes.H_INVOKESPECIAL,
//                                            Opcodes.H_NEWINVOKESPECIAL,
//                                            Opcodes.H_INVOKEINTERFACE
//                                        )
//                                    ) Handle(
//                                        tag,
//                                        mapClassName(owner, fromNamespace, toNamespace) ?: owner,
//                                        tree[owner]?.toCheck()?.firstNotNullOfOrNull {
//                                            mapMethodName(
//                                                it,
//                                                name,
//                                                desc,
//                                                fromNamespace,
//                                                toNamespace
//                                            )
//                                        } ?: name,
//                                        mapMethodDesc(desc, fromNamespace, toNamespace),
//                                        isInterface
//                                    ) else if (
//                                        tag.equalsAny(
//                                            Opcodes.H_GETFIELD,
//                                            Opcodes.H_GETSTATIC,
//                                            Opcodes.H_PUTFIELD,
//                                            Opcodes.H_PUTSTATIC
//                                        )
//                                    ) Handle(
//                                        tag,
//                                        mapClassName(owner, fromNamespace, toNamespace) ?: owner,
//                                        tree[owner]?.toCheck()?.firstNotNullOfOrNull {
//                                            mapFieldName(
//                                                it,
//                                                name,
//                                                fromNamespace,
//                                                toNamespace
//                                            )
//                                        } ?: name,
//                                        mapType(desc, fromNamespace, toNamespace),
//                                        isInterface
//                                    ) else throw IllegalArgumentException("Unknown tag type : '$tag' for invoke dynamic instruction : '$insnNode' with handle: '$this'")
//                                }
//
//                                // Type and Handle
//                                insnNode.bsm = insnNode.bsm.mapHandle()
//
//                                insnNode.bsmArgs = insnNode.bsmArgs.map {
//                                    when (it) {
//                                        is Type -> {
//                                            when (it.sort) {
//                                                Type.ARRAY, Type.OBJECT -> Type.getType(
//                                                    mapType(
//                                                        it.internalName,
//                                                        fromNamespace,
//                                                        toNamespace
//                                                    )
//                                                )
//
//                                                Type.METHOD -> Type.getType(
//                                                    mapMethodDesc(
//                                                        it.internalName,
//                                                        fromNamespace,
//                                                        toNamespace
//                                                    )
//                                                )
//
//                                                else -> it
//                                            }
//                                        }
//
//                                        is Handle -> it.mapHandle()
//                                        else -> it
//                                    }
//                                }.toTypedArray()
//
//                                // Can ignore name because only the name of the bootstrap method is known at compile time and that is held in the handle field
//                                insnNode.desc =
//                                    mapMethodDesc(
//                                        insnNode.desc,
//                                        fromNamespace,
//                                        toNamespace
//                                    ) // Expected descriptor type of the generated call site
//                            }
//
//                            is MethodInsnNode -> {
//                                if (insnNode.owner.startsWith("[")) {
//                                    insnNode.owner = mapType(insnNode.owner, fromNamespace, toNamespace)
//                                } else {
//                                    insnNode.name = tree[insnNode.owner]?.toCheck()?.firstNotNullOfOrNull {
//                                        mapMethodName(
//                                            it,
//                                            insnNode.name,
//                                            insnNode.desc,
//                                            fromNamespace,
//                                            toNamespace
//                                        )
//                                    } ?: insnNode.name
//
//                                    insnNode.owner =
//                                        mapClassName(insnNode.owner, fromNamespace, toNamespace) ?: insnNode.owner
//                                    insnNode.desc = mapMethodDesc(insnNode.desc, fromNamespace, toNamespace)
//                                }
//                            }
//
//                            is MultiANewArrayInsnNode -> {
//                                insnNode.desc = mapType(insnNode.desc, fromNamespace, toNamespace)
//                            }
//
//                            is TypeInsnNode -> {
//                                insnNode.desc = if (insnNode.desc.startsWith("[")) mapType(
//                                    insnNode.desc,
//                                    fromNamespace,
//                                    toNamespace
//                                ) else mapClassName(insnNode.desc, fromNamespace, toNamespace) ?: insnNode.desc
//                            }
//
//                            is LdcInsnNode -> {
//                                when (val it = insnNode.cst) {
//                                    is Type -> {
//                                        insnNode.cst = when (it.sort) {
//                                            Type.ARRAY, Type.OBJECT -> Type.getType(
//                                                mapType(
//                                                    it.internalName,
//                                                    fromNamespace,
//                                                    toNamespace
//                                                )
//                                            )
//
//                                            Type.METHOD -> Type.getType(
//                                                mapMethodDesc(
//                                                    it.internalName,
//                                                    fromNamespace,
//                                                    toNamespace
//                                                )
//                                            )
//
//                                            else -> it
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//                classNode.name = mapClassName(classNode.name, fromNamespace, toNamespace) ?: classNode.name
//
//                if (classNode.signature != null)
//                    classNode.signature = mapAnySignature(classNode.signature, fromNamespace, toNamespace)
//
//                classNode.interfaces = classNode.interfaces.map { mapClassName(it, fromNamespace, toNamespace) ?: it }
//
//                classNode.outerClass =
//                    if (classNode.outerClass != null) mapClassName(classNode.outerClass, fromNamespace, toNamespace)
//                        ?: classNode.outerClass else null
//
//                classNode.superName =
//                    mapClassName(classNode.superName, fromNamespace, toNamespace) ?: classNode.superName
//
//                classNode.nestHostClass =
//                    if (classNode.nestHostClass != null) mapClassName(
//                        classNode.nestHostClass,
//                        fromNamespace,
//                        toNamespace
//                    )
//                        ?: classNode.nestHostClass else null
//
//                classNode.nestMembers = classNode.nestMembers?.map {
//                    mapClassName(it, fromNamespace, toNamespace) ?: it
//                } ?: ArrayList()
//
//                classNode.innerClasses.forEach { n ->
//                    n.outerName =
//                        if (n.outerName != null) mapClassName(n.outerName, fromNamespace, toNamespace)
//                            ?: n.outerName else n.outerName
//                    n.name = mapClassName(n.name, fromNamespace, toNamespace) ?: n.name
//                    n.innerName = if (n.innerName != null) n.name.substringAfter("\$") else null
//                }
//
//                if (classNode.recordComponents != null) classNode.recordComponents.forEach {
//                    it.descriptor = mapType(it.descriptor, fromNamespace, toNamespace)
//                    if (it.signature != null) it.signature = mapAnySignature(it.signature, fromNamespace, toNamespace)
//                }
//
//                if (classNode.permittedSubclasses != null)
//                    classNode.permittedSubclasses = classNode.permittedSubclasses.map {
//                        mapClassName(it, fromNamespace, toNamespace)
//                    }
//            }
//        }
//    }
}


// Archive transforming context
public data class ArchiveTransformerContext(
    val theArchive: ArchiveReference,
    val dependencies: List<ArchiveTree>,
    val mappings: ArchiveMapping,
    val fromNamespace: String,
    val toNamespace: String,
    val inheritanceTree: ClassInheritanceTree
)

internal fun InputStream.classNode(parsingOptions: Int = 0): ClassNode {
    val node = ClassNode()
    ClassReader(this).accept(node, parsingOptions)
    return node
}

private fun ArchiveReference.transformAndWriteClass(
    name: String, // Fake location
    context: ArchiveTransformerContext,
    config: TransformerConfig
) {
    val entry = checkNotNull(
        reader["$name.class"]
    ) { "Failed to find class '$name' when transforming archive: '${this.name}'" }

    val resolve = Archives.resolve(
        ClassReader(entry.resource.open()),
        config,
        MappingAwareClassWriter(
            config,
            context
        ),
        ClassReader.EXPAND_FRAMES
    )

    val transformedName = context.mappings.mapClassName(name, context.fromNamespace, context.toNamespace) ?: name
    val transformedEntry = ArchiveReference.Entry(
        "$transformedName.class",
        ProvidedResource(entry.resource.uri) {
            resolve
        },
        false,
        this
    )

    if (transformedName != name)
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
    private val config: TransformerConfig,
    private val context: ArchiveTransformerContext
) : AwareClassWriter(
    context.dependencies,
    0,
) {
    private fun getMappedNode(name: String): HierarchyNode? {
        var node = context.theArchive.reader["$name.class"]
            ?.resource?.open()?.classNode(ClassReader.EXPAND_FRAMES) ?: run {
            val otherName =
                // Switch it as we want to go the other direction
                context.mappings.mapClassName(name, context.toNamespace, context.fromNamespace) ?: return@run null
            context.theArchive.reader["$otherName.class"]?.resource?.open()
        }?.classNode(ClassReader.EXPAND_FRAMES) ?: return null

        node = config.ct(node)
        node.methods = node.methods.map {
            config.mt.invoke(it)
        }
        node.fields = node.fields.map {
            config.ft.invoke(it)
        }

        return UnloadedClassNode(node)
    }

    override fun loadType(name: String): HierarchyNode {
        return getMappedNode(name) ?: super.loadType(name)
    }
}

public typealias ClassInheritanceTree = Map<String, ClassInheritancePath>

public data class ClassInheritancePath(
    val name: String,
    val superClass: ClassInheritancePath?,
    val interfaces: List<ClassInheritancePath>
)

public fun createFakeInheritancePath(
    entry: ArchiveReference.Entry,
    reader: ArchiveReference.Reader
): ClassInheritancePath {
    val node = entry.resource.open().classNode()

    return ClassInheritancePath(
        node.name,

        reader[node.superName + ".class"]?.let { createFakeInheritancePath(it, reader) },
        node.interfaces?.mapNotNull { n ->
            reader["$n.class"]?.let { createFakeInheritancePath(it, reader) }
        } ?: listOf()
    )
}

public class DelegatingArchiveReader(
    private val archives: List<ArchiveReference>
) : ArchiveReference.Reader {
    override fun entries(): Sequence<ArchiveReference.Entry> = sequence {
        archives.forEach {
            yieldAll(it.reader.entries())
        }
    }

    override fun of(name: String): ArchiveReference.Entry? {
        return archives.firstNotNullOfOrNull {
            it.reader.of(name)
        }
    }
}

public fun createFakeInheritanceTree(reader: ArchiveReference.Reader): ClassInheritanceTree {
    return reader.entries()
        .filterNot(ArchiveReference.Entry::isDirectory)
        .filter { it.name.endsWith(".class") }
        .map { createFakeInheritancePath(it, reader) }
        .associateBy { it.name }
}

public fun transformArchive(
    archive: ArchiveReference,
    dependencies: List<ArchiveTree>,
    mappings: ArchiveMapping,
    fromNamespace: String,
    toNamespace: String,
) {
    val inheritanceTree: ClassInheritanceTree = createFakeInheritanceTree(archive.reader)

    val context = ArchiveTransformerContext(archive, dependencies, mappings, fromNamespace, toNamespace, inheritanceTree)

    val config = mappingTransformConfigFor(context)

    archive.reader.entries()
        .filter { it.name.endsWith(".class") }
        .forEach { e ->
            archive.transformAndWriteClass(e.name.removeSuffix(".class"), context, config)
        }
}