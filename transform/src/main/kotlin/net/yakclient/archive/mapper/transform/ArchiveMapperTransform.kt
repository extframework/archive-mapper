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
    direction: MappingDirection,
    tree: ClassInheritanceTree
): TransformerConfig.Mutable {
    fun ClassInheritancePath.toCheck(): List<String> {
        return listOf(name) + interfaces.flatMap { it.toCheck() } + (superClass?.toCheck() ?: listOf())
    }

    return TransformerConfig.of {
        transformClass { classNode: ClassNode ->
            mappings.run {
                classNode.fields.forEach { fieldNode ->
                    fieldNode.desc = mapType(fieldNode.desc, direction)
                    fieldNode.name = mapFieldName(classNode.name, fieldNode.name, direction) ?: fieldNode.name
                    if (fieldNode.signature != null)
                        fieldNode.signature = mapAnySignature(fieldNode.signature, direction)
                }

                classNode.methods.forEach { methodNode ->
                    // Mapping other references
                    methodNode.name = mapMethodName(classNode.name, methodNode.name, methodNode.desc, direction)
                        ?: (classNode.interfaces + classNode.superName).filterNotNull().firstNotNullOfOrNull { n ->
                            mapMethodName(n, methodNode.name, methodNode.desc, direction)
                        } ?: methodNode.name

                    methodNode.desc = mapMethodDesc(methodNode.desc, direction)

                    if (methodNode.signature != null)
                        methodNode.signature = mapAnySignature(methodNode.signature, direction)

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
                                insnNode.name = tree[insnNode.owner]?.toCheck()?.firstNotNullOfOrNull {
                                    mapFieldName(
                                        it,
                                        insnNode.name,
                                        direction
                                    )
                                } ?: insnNode.name

                                insnNode.owner = mapClassName(insnNode.owner, direction) ?: insnNode.owner
                                insnNode.desc = mapType(insnNode.desc, direction)
                            }

                            is InvokeDynamicInsnNode -> {
                                fun Handle.mapHandle(): Handle = Handle(
                                    tag,
                                    mapType(owner, direction),
                                    tree[owner]?.toCheck()?.firstNotNullOfOrNull {
                                        mapMethodName(
                                            it,
                                            name,
                                            desc,
                                            direction
                                        )
                                    } ?: name,
                                    mapMethodDesc(desc, direction),
                                    isInterface
                                )

                                // Type and Handle
                                insnNode.bsm = insnNode.bsm.mapHandle()

                                // TODO map bsm args

                                // Can ignore name because only the name of the bootstrap method is known at compile time and that is held in the handle field
                                insnNode.desc =
                                    mapMethodDesc(
                                        insnNode.desc,
                                        direction
                                    ) // Expected descriptor type of the generated call site
                            }

                            is MethodInsnNode -> {
                                insnNode.name = tree[insnNode.owner]?.toCheck()?.firstNotNullOfOrNull {
                                    mapMethodName(
                                        it,
                                        insnNode.name,
                                        insnNode.desc,
                                        direction
                                    )
                                } ?: insnNode.name

                                insnNode.owner = mapClassName(insnNode.owner, direction) ?: insnNode.owner
                                insnNode.desc = mapMethodDesc(insnNode.desc, direction)
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
                classNode.name = mapClassName(classNode.name, direction) ?: classNode.name

                if (classNode.signature != null)
                    classNode.signature = mapAnySignature(classNode.signature, direction)

                classNode.interfaces = classNode.interfaces.map { mapClassName(it, direction) ?: it }

                classNode.outerClass = if (classNode.outerClass != null) mapClassName(classNode.outerClass, direction)
                    ?: classNode.outerClass else null

                classNode.superName = mapClassName(classNode.superName, direction) ?: classNode.superName

                classNode.nestHostClass =
                    if (classNode.nestHostClass != null) mapClassName(classNode.nestHostClass, direction)
                        ?: classNode.nestHostClass else null

                classNode.nestMembers = classNode.nestMembers?.map {
                    mapClassName(it, direction) ?: it
                } ?: ArrayList()

                classNode.innerClasses.forEach { n ->
                    n.outerName =
                        if (n.outerName != null) mapClassName(n.outerName, direction) ?: n.outerName else n.outerName
                    n.name = mapClassName(n.name, direction) ?: n.name
                    n.innerName = if (n.innerName != null) n.name.substringAfter("\$") else null
                }
            }
        }
    }
}


// Archive transforming context
private data class ATContext(
    val theArchive: ArchiveReference,
    val dependencies: List<ArchiveTree>,
    val mappings: ArchiveMapping,
    val config: TransformerConfig,
    val direction: MappingDirection
) {
    fun make(): ClassWriter = MappingAwareClassWriter(
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

    if (transformedNode.name != name)
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
    private fun getMappedNode(name: String): HierarchyNode? {
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
    val node = entry.resource.open().parseNode

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
    direction: MappingDirection,
) {
    val inheritanceTree: ClassInheritanceTree = createFakeInheritanceTree(archive.reader)

    val config = mappingTransformConfigFor(mappings, direction, inheritanceTree)

    val context = ATContext(archive, dependencies, mappings, config, direction)

    archive.reader.entries()
        .filter { it.name.endsWith(".class") }
        .forEach { e ->
            archive.transformAndWriteClass(e.name.removeSuffix(".class"), context)
        }
}