package net.yakclient.archive.mapper.transform

import com.durganmcbroom.resources.openStream
import com.durganmcbroom.resources.streamToResource
import net.yakclient.archive.mapper.ArchiveMapping
import net.yakclient.archives.ArchiveReference
import net.yakclient.archives.ArchiveTree
import net.yakclient.archives.Archives
import net.yakclient.archives.transform.AwareClassWriter
import net.yakclient.archives.transform.TransformerConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
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
        ClassReader(entry.resource.openStream()),
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
        streamToResource(entry.resource.location) {
            ByteArrayInputStream(resolve)
        },
        false,
        this
    )

    if (transformedName != name)
        writer.remove("$name.class")
    writer.put(transformedEntry)
}

private class MappingAwareClassWriter(
    private val config: TransformerConfig,
    private val context: ArchiveTransformerContext
) : AwareClassWriter(
    context.dependencies,
    0,
) {
    private fun getMappedNode(name: String): HierarchyNode? {
        var node = context.theArchive.reader["$name.class"]
            ?.resource?.openStream()?.classNode(ClassReader.EXPAND_FRAMES) ?: run {
            val otherName =
                // Switch it as we want to go the other direction
                context.mappings.mapClassName(name, context.toNamespace, context.fromNamespace) ?: return@run null
            context.theArchive.reader["$otherName.class"]?.resource?.openStream()
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
    val node = entry.resource.openStream().classNode()

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