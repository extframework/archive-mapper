package net.yakclient.archive.mapper.parsers.tiny

import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.yakclient.archive.mapper.*
import net.yakclient.archives.extension.parameters
import net.yakclient.archives.transform.MethodSignature
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

public interface TinyReader {
    public fun read(reader: Reader, visitor: MappingVisitor)
}

public open class TinyMappingsParser(
    protected open val reader: TinyReader
) : MappingParser {
    override val name: String = "tinyV2"
    private fun <I : MappingIdentifier, T : MappingNode<I>> List<T>.toBiMap(): Map<I, T> {
        val realMap = associateBy { it.realIdentifier }
        val fakeMap = associateBy { it.fakeIdentifier }

        return realMap + fakeMap
    }

    override final fun parse(mappingsIn: InputStream): ArchiveMapping {
        val tree = MemoryMappingTree()
        reader.read(BufferedReader(InputStreamReader(mappingsIn)), tree)

        return ArchiveMapping(
             tree.classes.mapNotNull mapClasses@ {
                ClassMapping(
                    ClassIdentifier(
                        it.getName("intermediary") ?: return@mapClasses null,
                        MappingType.REAL
                    ),
                    ClassIdentifier(
                        it.getName("official") ?: return@mapClasses null,
                        MappingType.FAKE
                    ),
                    it.methods.mapNotNull mapMethods@ { m ->
                        val intermediaryDesc = MethodSignature.of(m.getDesc("intermediary") ?: return@mapMethods null)
                        val officialDesc = MethodSignature.of(m.getDesc("official") ?: return@mapMethods null)
                        MethodMapping(
                            MethodIdentifier(
                                m.getName("intermediary") ?: return@mapMethods null,
                                parameters(intermediaryDesc.desc)
                                    .map(::fromInternalType),
                                MappingType.REAL
                            ),
                            MethodIdentifier(
                                m.getName("official") ?: return@mapMethods null,
                                parameters(officialDesc.desc)
                                    .map(::fromInternalType),
                                MappingType.FAKE
                            ),
                            null, null, null, null,
                            fromInternalType(intermediaryDesc.returnType!!),
                            fromInternalType(officialDesc.returnType!!),
                        )
                    }.toBiMap(),
                    it.fields.mapNotNull mapFields@ {f ->
                        FieldMapping(
                            FieldIdentifier(
                                f.getName("intermediary") ?: return@mapFields null,
                                MappingType.REAL,
                            ),
                            FieldIdentifier(
                                f.getName("official") ?: return@mapFields null,
                                MappingType.FAKE,
                            ),
                            fromInternalType(f.getDesc("intermediary") ?: return@mapFields null),
                            fromInternalType(f.getDesc("official") ?: return@mapFields null),
                        )
                    }.toBiMap()
                )
            }.toBiMap()
        )
    }
}