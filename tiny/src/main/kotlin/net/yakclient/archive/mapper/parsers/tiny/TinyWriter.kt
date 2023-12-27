@file:JvmName("TinyWriter")

package net.yakclient.archive.mapper.parsers.tiny

import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.yakclient.archive.mapper.ArchiveMapping
import net.yakclient.archive.mapper.MappingType


// Namespace 0 = dstNamespace
public fun <T> write(writer1: T, mappings: ArchiveMapping, srcNamespace: String, dstNamespace: String)
        where T : MappingVisitor, T : AutoCloseable {
    writer1.use { writer ->
        writer.visitHeader()
        writer.visitNamespaces(srcNamespace, listOf(dstNamespace))
        writer.visitContent()
        mappings.classes.asSequence().filter { it.key.type == MappingType.REAL }.forEach { (_, clazz) ->
            writer.visitClass(clazz.realIdentifier.name)
            writer.visitDstName(MappedElementKind.CLASS, 0, clazz.fakeIdentifier.name)
            writer.visitElementContent(MappedElementKind.CLASS)

            clazz.methods.asSequence().filter { it.key.type == MappingType.REAL }.forEach { (_, method) ->
                writer.visitMethod(method.realIdentifier.name,
                    "(" + method.realIdentifier.parameters.joinToString(separator = "") {
                        it.descriptor
                    } + ")" + method.realReturnType.descriptor)
                writer.visitDstName(MappedElementKind.METHOD, 0, method.fakeIdentifier.name)
                writer.visitDstDesc(MappedElementKind.METHOD,
                    0,
                    "(" + method.fakeIdentifier.parameters.joinToString(separator = "") {
                        it.descriptor
                    } + ")" + method.fakeReturnType.descriptor)

                method.realIdentifier.parameters.zip(method.fakeIdentifier.parameters).withIndex()
                    .forEach { (i, pair) ->
                        val (r, f) = pair
                        writer.visitMethodArg(i, i, r.descriptor)
                        writer.visitDstName(MappedElementKind.METHOD_ARG, 0, f.descriptor)
                    }
                writer.visitElementContent(MappedElementKind.METHOD)
            }

            clazz.fields.asSequence().filter { it.key.type == MappingType.REAL }.forEach { (_, field) ->
                writer.visitField(field.realIdentifier.name, field.realType.descriptor)
                writer.visitDstName(MappedElementKind.FIELD, 0, field.fakeIdentifier.name)
                writer.visitDstDesc(MappedElementKind.FIELD, 0, field.fakeType.descriptor)
                writer.visitElementContent(MappedElementKind.FIELD)

            }
        }
    }
}
