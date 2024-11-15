package dev.extframework.archive.mapper.test

import dev.extframework.archive.mapper.*
import dev.extframework.archive.mapper.parsers.proguard.ProGuardMappingParser
import dev.extframework.archive.mapper.parsers.tiny.TinyV1MappingsParser
import org.objectweb.asm.Type
import java.net.URL
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.expect

// TODO
class TestMappingGraph {
    val namespaces1 = setOf("real", "fake")


    val mapping1 = ArchiveMapping(
        namespaces1,
        listOf(
            ArchiveIdentifier("", "real"),
            ArchiveIdentifier("", "fake")
        ).toMap(),
        listOf(
            ClassMapping(
                namespaces1,
                listOf(
                    ClassIdentifier("FirstReal", "real"),
                    ClassIdentifier("FirstFake", "fake"),
                ).toMap(),

                listOf(
                    MethodMapping(
                        namespaces1,
                        listOf(

                            MethodIdentifier("FirstRealMethod", listOf(), "real"),
                            MethodIdentifier("FirstFakeMethod", listOf(), "fake"),
                        ).toMap(),
                        null, null,

                        toValues(
                            "real" to Type.BOOLEAN_TYPE,
                            "fake" to Type.INT_TYPE
                        ),
                    )
                ).toMap(),

                listOf(
                    FieldMapping(
                        namespaces1,
                        listOf(
                            FieldIdentifier("FirstRealField", "real"),
                            FieldIdentifier("FirstFakeField", "fake"),
                        ).toMap(),
                        toValues(
                            "real" to Type.BOOLEAN_TYPE,
                            "fake" to Type.INT_TYPE
                        )
                    )
                ).toMap()
            )
        ).toMap()
    )

    val namespaces2 = setOf("real", "fake2")

    val mapping2 = ArchiveMapping(
        namespaces2,
        listOf(
            ArchiveIdentifier("", "real"),
            ArchiveIdentifier("", "fake2")
        ).toMap(),
        listOf(
            ClassMapping(
                namespaces2,
                listOf(
                    ClassIdentifier("FirstReal", "real"),
                    ClassIdentifier("SecondFake", "fake2"),
                ).toMap(),
                listOf(
                    MethodMapping(
                        namespaces2,
                        listOf(
                            MethodIdentifier("FirstRealMethod", listOf(), "real"),
                            MethodIdentifier("SecondFakeMethod", listOf(), "fake2"),
                        ).toMap(),
                        null, null,
                        toValues(
                            "real" to Type.BOOLEAN_TYPE,
                            "fake2" to Type.INT_TYPE
                        )
                    )
                ).toMap(),
                listOf(
                    FieldMapping(
                        namespaces2,
                        listOf(
                            FieldIdentifier("FirstRealField", "real"),
                            FieldIdentifier("SecondFakeField", "fake2"),
                        ).toMap(),
                        toValues(
                            "real" to Type.BOOLEAN_TYPE,
                            "fake2" to Type.INT_TYPE
                        )
                    )
                ).toMap()
            )
        ).toMap()
    )

    val namespaces3 = setOf("fake2", "real3")

    val mapping3 = ArchiveMapping(
        namespaces3,
        listOf(
            ArchiveIdentifier("", "fake2"),
            ArchiveIdentifier("", "real3")
        ).toMap(),
        listOf(
            ClassMapping(
                namespaces3,
                listOf(
                    ClassIdentifier("SecondFake", "fake2"),
                    ClassIdentifier("ThirdFake", "real3"),
                ).toMap(),

                listOf(
                    MethodMapping(
                        namespaces3,
                        listOf(

                            MethodIdentifier("SecondFakeMethod", listOf(), "fake2"),
                            MethodIdentifier("ThirdFakeMethod", listOf(), "real3"),
                        ).toMap(),
                        null, null,
                        toValues(
                            "fake2" to Type.BOOLEAN_TYPE,
                            "real3" to Type.INT_TYPE
                        )
                    )
                ).toMap(),
                listOf(
                    FieldMapping(
                        namespaces3,
                        listOf(
                            FieldIdentifier("SecondFakeField", "fake2"),
                            FieldIdentifier("ThirdFakeField", "real3"),
                        ).toMap(),
                        toValues(
                            "fake2" to Type.BOOLEAN_TYPE,
                            "real3" to Type.BOOLEAN_TYPE
                        )
                    )
                ).toMap()
            )
        ).toMap()
    )

    val expectedNamespaces = setOf("real3", "fake")

    val expected = ArchiveMapping(
        expectedNamespaces,
        listOf(
            ArchiveIdentifier("", "real3"),
            ArchiveIdentifier("", "fake")
        ).toMap(),
        listOf(
            ClassMapping(
                expectedNamespaces,
                listOf(

                    ClassIdentifier("ThirdFake", "real3"),
                    ClassIdentifier("FirstFake", "fake"),
                ).toMap(),

                listOf(
                    MethodMapping(
                        expectedNamespaces,
                        listOf(
                            MethodIdentifier("ThirdFakeMethod", listOf(), "real3"),
                            MethodIdentifier("FirstFakeMethod", listOf(), "fake"),
                        ).toMap(),
                        null, null,
                        toValues(
                            "real3" to Type.INT_TYPE,
                            "fake" to Type.INT_TYPE
                        )
                    )
                ).toMap(),
                listOf(
                    FieldMapping(
                        expectedNamespaces,
                        listOf(
                            FieldIdentifier("ThirdFakeField", "real3"),
                            FieldIdentifier("FirstFakeField", "fake"),
                        ).toMap(),
                        toValues(
                            "real3" to Type.BOOLEAN_TYPE,
                            "fake" to Type.INT_TYPE
                        )
                    )
                ).toMap()
            )
        ).toMap()
    )

    private fun <T> toValues(vararg pairs: Pair<String, T>): MappingValueContainer<T> {
        return MappingValueContainerImpl(
            pairs.toMap()
        )
    }

    private fun <V : MappingIdentifier> List<V>.toMap(): MappingValueContainer<V> {
        return MappingValueContainerImpl(associateBy { it.namespace })
    }

    private fun <K : MappingIdentifier, V : MappingNode<K>> List<V>.toMap(): MappingNodeContainer<K, V> {
        return MappingNodeContainerImpl(toSet())
//        return MappingValueContainerImpl(this.flatMap {
//            val namespaces = it.namespaces.toList()
//            listOf(
//                namespaces[0] to it,
//                namespaces[1] to it
//            )
//        }.associate { it })
    }

    //
    @Test
    fun `Test joining 1 mappings returns same thing`() {
        val path = listOf(
            DirectedMappingNode(
                DirectedMappingType("fake"),
                DirectedMappingType("real"),
                mapping1
            )
        )

        val joined = joinMappings(path)
        check(joined == mapping1)
    }

    @Test
    fun `Test joining multiple mappings returns correctly`() {
        val path = listOf(
            DirectedMappingNode(
                DirectedMappingType("fake"),
                DirectedMappingType("real"),
                mapping1
            ),
            DirectedMappingNode(
                DirectedMappingType("real"),
                DirectedMappingType("fake2"),
                mapping2
            ),
            DirectedMappingNode(
                DirectedMappingType("fake2"),
                DirectedMappingType("real3"),
                mapping3
            )
        )

        val joined = joinMappings(path)

        check(joined == expected)
    }
//
    fun createProvider(
        real: String,
        fake: String,
        mappingProvider: (String) -> ArchiveMapping? = { null }
    ): MappingsProvider = object : MappingsProvider {
        override val namespaces: Set<String> = setOf(real, fake)

        override fun forIdentifier(identifier: String): ArchiveMapping {
            return mappingProvider(identifier) ?: throw IllegalStateException("This is not suppose to be called!")
        }
    }

    @Test
    fun `Test graph produces correct edges`() {
        val graph = newMappingsGraph(
            listOf(
                createProvider("First", "Second"),
                createProvider("Second", "Third"),
                createProvider("First", "Fourth"),
                createProvider("Third", "Fourth"),
            )
        )

        fun assertEdges(vertex: String, vararg outgoing: String) {
            val edges = graph.connectingEdges(vertex)

            outgoing.forEach { out ->
                check(edges.any {
                    it.to == out || it.from == out
                })
            }
        }

        assertEdges(
            "First",
            "Second",
            "Fourth"
        )

        assertEdges(
            "Second",
            "First",
            "Third"
        )

        assertEdges(
            "Fourth",
            "First",
            "Third"
        )
    }

    @Test
    fun `Test graph and archive creation`() {
        val graph = newMappingsGraph(
            listOf(
                createProvider("real", "fake") { mapping1 },
                createProvider("real", "fake2") { mapping2 },
                createProvider("fake2", "real3") { mapping3 }
            )
        )

        val output = graph.findShortest("fake", "real3")

        check(output.forIdentifier("") == expected)
    }

    @Test
    fun `Test graph and archive creation (2)`() {
        val graph = newMappingsGraph(
            listOf(
                createProvider("official", "intermediary") { mapping1 },
                createProvider("official", "official(deobf)") { mapping2 },
            )
        )

        val output = graph.findShortest("intermediary", "official(deobf)")
    }

    @Test
    fun `Test fabric mappings work`() {
        val intermediaryProvider = object : MappingsProvider {
            override val namespaces: Set<String> = setOf("intermediary", "official")

            override fun forIdentifier(identifier: String): ArchiveMapping {
                return TinyV1MappingsParser.parse(
                    URL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/$identifier.tiny").openStream()
                )
            }
        }

        val officialProvider = object : MappingsProvider {
            override val namespaces: Set<String> = setOf("official(deobf)", "official")

            override fun forIdentifier(identifier: String): ArchiveMapping {
                check(identifier == "1.21.1")
                return ProGuardMappingParser("official", "official(deobf)").parse(
                    URL("https://piston-data.mojang.com/v1/objects/2244b6f072256667bcd9a73df124d6c58de77992/client.txt").openStream()
                )
            }
        }

        val graph = newMappingsGraph(
            listOf(
                officialProvider,
                intermediaryProvider
            ),
            false
        )

        val provider = graph.findShortest(
             "official(deobf)", "intermediary",
        )

        val forIdentifier = provider.forIdentifier("1.21.1")
        println("")
    }
}