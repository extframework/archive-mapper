package net.yakclient.archive.mapper.parsers.tiny

import net.fabricmc.mapping.tree.MethodDef
import net.fabricmc.mapping.tree.TinyMappingFactory
import net.yakclient.archive.mapper.*
import net.yakclient.archives.extension.parameters
import net.yakclient.archives.transform.MethodSignature
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

public object TinyV2MappingsParser : MappingParser {
    override val name: String = "tinyV2"
    private fun <I : MappingIdentifier, T : MappingNode<I>> List<T>.toBiMap(): Map<I, T> {
        val realMap = associateBy { it.realIdentifier }
        val fakeMap = associateBy { it.fakeIdentifier }

        return realMap + fakeMap
    }

    override fun parse(mappingsIn: InputStream): ArchiveMapping {
        val tinyTree = TinyMappingFactory.loadWithDetection(BufferedReader(InputStreamReader(mappingsIn)))

        return ArchiveMapping(
            tinyTree.classes.map {
                ClassMapping(
                    ClassIdentifier(
                        it.getName("intermediary"),
                        MappingType.REAL
                    ),
                    ClassIdentifier(
                        it.getName("official"),
                        MappingType.FAKE
                    ),
                    it.methods.map { m: MethodDef ->
                        val intermediaryDesc = MethodSignature.of(m.getDescriptor("intermediary"))
                        val officialDesc = MethodSignature.of(m.getDescriptor("official"))
                        MethodMapping(
                            MethodIdentifier(
                                m.getName("intermediary"),
                                parameters(intermediaryDesc.desc)

                                    .map(::fromInternalType),
                                MappingType.REAL
                            ),
                            MethodIdentifier(
                                m.getName("official"),
                                parameters(officialDesc.desc)
                                    .map(::fromInternalType),
                                MappingType.FAKE
                            ),
                            null, null, null, null,
                            fromInternalType(intermediaryDesc.returnType!!),
                            fromInternalType(officialDesc.returnType!!),
                        )
                    }.toBiMap(),
                    it.fields.map {f ->
                        FieldMapping(
                            FieldIdentifier(
                                f.getName("intermediary"),
                                MappingType.REAL,
                            ),
                            FieldIdentifier(
                                f.getName("official"),
                                MappingType.FAKE,
                            ),
                            fromInternalType(f.getDescriptor("intermediary")),
                            fromInternalType(f.getDescriptor("official")),
                        )
                    }.toBiMap()
                )
            }.toBiMap()
        )
    }
}