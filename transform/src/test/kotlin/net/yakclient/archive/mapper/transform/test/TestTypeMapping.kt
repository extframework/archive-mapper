package net.yakclient.archive.mapper.transform.test

import net.yakclient.archive.mapper.*
import net.yakclient.archive.mapper.parsers.proguard.ProGuardMappingParser
import net.yakclient.archive.mapper.transform.*
import net.yakclient.archives.Archives
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.net.URL
import kotlin.test.Test

class TestTypeMapping {
    private fun printAndCheck(any: Any, other: Any) {
        println(any)
        check(any == other)
    }

    @Test
    fun `Test single type mapping`() {
        val parser = ProGuardMappingParser("official", "named")

        val mappings =
            parser.parse(URL("https://launcher.mojang.com/v1/objects/a661c6a55a0600bd391bdbbd6827654c05b2109c/client.txt").openStream())

        printAndCheck(mappings.mapType("C",  "named","official"), "C")
        printAndCheck(mappings.mapType("Ljava/lang/String;", "named","official"), "Ljava/lang/String;")
        printAndCheck(
            mappings.mapType("Lcom/mojang/blaze3d/platform/InputConstants;", "named","official"),
            "Ldsh;"
        )
        printAndCheck(
            mappings.mapType("[[[Lcom/mojang/blaze3d/platform/InputConstants;", "named","official"),
            "[[[Ldsh;"
        )
    }

    @Test
    fun `Test map signature`() {
        val parser = ProGuardMappingParser("official", "named")

        val mappings =
            parser.parse(this::class.java.getResourceAsStream("/minecraft-mappings-1.20.1.txt")!!)

//        printAndCheck(
//            mappings.mapAnySignature(
//                "([Ldwi\$a;IILdwi\$a<IILdvx;>;)Ljava/util/List<Ldwl;>;",
//                "official",
//                "named"
//            ),
//            "([Lnet/minecraft/world/level/timers/TimerQueue\$Event;IILnet/minecraft/world/level/timers/TimerQueue\$Event<IILnet/minecraft/world/level/storage/loot/providers/score/ContextScoreboardNameProvider;>;)Ljava/util/List<Lnet/minecraft/world/phys/AABB;>;"
//        )
    }

//    @Test
//    fun `Test type with generic regex`() {
//        val typeWithGenericRegex = Regex("^(.+?)(?:<(.+?)>)?;$")
//
//        fun testStr(str: String, vararg with: String) {
//            val match = typeWithGenericRegex.matchEntire(str)
//            checkNotNull(match)
//            println(match.groupValues)
//            check(match.groupValues.containsAll(listOf(*with)))
//        }
//
//
//        testStr("Lasdfasdfasdf<asdfasdf;>;", "asdfasdfasdf", "asdfasdf;")
//        testStr("Lasdfasdf;", "asdfasdf")
//        testStr("[Lasdfasdf;", "asdfasdf")
//        testStr("[[[[Laaaa<asdf>;", "aaaa", "asdf")
//
//    }

//    public fun transformClass(
//        reader: ClassReader,
//        mappings: ArchiveMapping,
//        fromNamespace: String,
//        toNamespace: String,
//        writer: ClassWriter = ClassWriter(Archives.WRITER_FLAGS)
//    ): ByteArray {
//        return Archives.resolve(
//            reader,
//            mappingTransformConfigFor(mappings,fromNamespace, toNamespace,
//                listOf(
//                    ClassInheritancePath("net/yakclient/archive/mapper/transform/test/FakeClass", null, listOf()),
//                    ClassInheritancePath("net/yakclient/archive/mapper/transform/test/FakeException", null, listOf()),
//                    ClassInheritancePath("net/yakclient/archive/mapper/transform/test/FakeSuperType", null, listOf()),
//                ).associateBy { it.name }
//            ),
//            writer
//        )
//    }

//    @Test
//    fun `Test full class type map`() {
//        val namespaces = setOf("real", "fake")
//
//        val thingMethod = MethodMapping(
//            namespaces,
//            MappingValueContainerImpl(
//                mapOf(
//                    "real" to MethodIdentifier("doRealThing", listOf(), "real"),
//                    "fake" to MethodIdentifier("doFakeThing", listOf(), "fake")
//                )
//            ),
//            null,
//            null,
//            MappingValueContainerImpl(
//                mapOf(
//                    "real" to PrimitiveTypeIdentifier.VOID,
//                    "fake" to PrimitiveTypeIdentifier.VOID
//                )
//            )
//        )
//
//        val somethingMethod = MethodMapping(
//            MethodIdentifier(
//                "doSomethingElse",
//                listOf(ArrayTypeIdentifier(ClassTypeIdentifier("java/lang/String"))),
//                REAL
//            ),
//            MethodIdentifier(
//                "doOtherFakeThing",
//                listOf(ArrayTypeIdentifier(ClassTypeIdentifier("java/lang/String"))),
//                FAKE
//            ),
//            null,
//            null,
//            null,
//            null,
//            PrimitiveTypeIdentifier.VOID,
//            PrimitiveTypeIdentifier.VOID,
//        )
//
//        val stringValueField = FieldMapping(
//            FieldIdentifier("realStringValue", REAL),
//            FieldIdentifier("fakeStringValue", FAKE),
//            ClassTypeIdentifier("java/lang/String"),
//            ClassTypeIdentifier("java/lang/String")
//
//        )
//        val classMapping = ClassMapping(
//            ClassIdentifier(
//                "net/yakclient/archive/mapper/transform/test/RealClass", REAL
//            ),
//            ClassIdentifier("net/yakclient/archive/mapper/transform/test/FakeClass", FAKE),
//            mapOf(
//                thingMethod.realIdentifier to thingMethod,
//                thingMethod.fakeIdentifier to thingMethod,
//                somethingMethod.realIdentifier to somethingMethod,
//                somethingMethod.fakeIdentifier to somethingMethod
//            ),
//
//            mapOf(
//                stringValueField.realIdentifier to stringValueField,
//                stringValueField.fakeIdentifier to stringValueField
//            )
//        )
//
//        val exceptionMapping = ClassMapping(
//            ClassIdentifier(
//                "net/yakclient/archive/mapper/transform/test/RealException", REAL
//            ),
//            ClassIdentifier("net/yakclient/archive/mapper/transform/test/FakeException", FAKE),
//            mapOf(),
//            mapOf()
//        )
//
//        val superTypeMethod = MethodMapping(
//            MethodIdentifier(
//                "getRealName", listOf(), REAL
//            ),
//            MethodIdentifier(
//                "getFakeName", listOf(), FAKE
//            ), null, null, null, null,
//            PrimitiveTypeIdentifier.VOID,
//            PrimitiveTypeIdentifier.VOID
//        )
//
//        val superTypeMapping = ClassMapping(
//            ClassIdentifier(
//                "net/yakclient/archive/mapper/transform/test/RealSuperType", REAL
//            ),
//            ClassIdentifier("net/yakclient/archive/mapper/transform/test/FakeSuperType", FAKE),
//            mapOf(
//                superTypeMethod.realIdentifier to superTypeMethod,
//                superTypeMethod.fakeIdentifier to superTypeMethod
//            ),
//            mapOf()
//        )
//
//        val mappings = ArchiveMapping(
//            mapOf(
//                classMapping.realIdentifier to classMapping,
//                classMapping.fakeIdentifier to classMapping,
//                exceptionMapping.realIdentifier to exceptionMapping,
//                exceptionMapping.fakeIdentifier to exceptionMapping,
//                superTypeMapping.realIdentifier to superTypeMapping,
//                superTypeMapping.fakeIdentifier to superTypeMapping
//            )
//        )
//
//
//        val resolve = transformClass(
//            ClassReader(ToTransform::class.java.name),
//            mappings,
//            MappingDirection.TO_FAKE
//        )
//
//        val classloader1 = object : ClassLoader(this::class.java.classLoader) {
//            var loaded = false
//            override fun loadClass(name: String?): Class<*> {
//                if (name == ToTransform::class.java.name && !loaded) {
//                    loaded = true
//
//                    return defineClass(ToTransform::class.java.name, resolve, 0, resolve.size)
//                }
//
//                return super.loadClass(name)
//            }
//        }
//
//        val cls1 = classloader1.loadClass(ToTransform::class.java.name)
//        cls1.getMethod("doSomething")
//            .also(Method::trySetAccessible)
//            .invoke(cls1.getConstructor().also(Constructor<*>::trySetAccessible).newInstance())
//
//        println(cls1.interfaces)
//
//        val classloader2 = object : ClassLoader(this::class.java.classLoader) {
//            var loaded = false
//            override fun loadClass(name: String?): Class<*> {
//                if (name == ToTransform::class.java.name && !loaded) {
//                    loaded = true
//
//                    val resolve = transformClass(
//                        ClassReader(resolve),
//                        mappings,
//                        MappingDirection.TO_REAL
//                    )
//
//                    return defineClass(ToTransform::class.java.name, resolve, 0, resolve.size)
//                }
//
//                return super.loadClass(name)
//            }
//        }
//
//        println("---------------------")
//        val cls2 = classloader2.loadClass(ToTransform::class.java.name)
//        cls2.getMethod("doSomething")
//            .also(Method::trySetAccessible)
//            .invoke(cls2.getConstructor().also(Constructor<*>::trySetAccessible).newInstance())
//    }
}

class FakeClass {
    @JvmField
    val fakeStringValue: String = "This is probably not real"

    fun doFakeThing() {
        println("This is fake!!")
    }

    fun doOtherFakeThing(vararg params: String) {
        println(params.joinToString() + " This is fake!")
    }
}

class RealClass {
    @JvmField
    val realStringValue: String = "This is real"

    fun doRealThing() {
        println("This is real!")
    }

    fun doSomethingElse(vararg params: String) {
        println(params.joinToString())
    }
}

data class FakeException(
    override val message: String
) : Exception()

data class RealException(
    override val message: String
) : Exception()

interface RealSuperType {
    val realName: String
}

interface FakeSuperType {
    val fakeName: String
}

class ToTransform(
    val value: RealClass = RealClass()
) : RealSuperType {
    fun doSomething() {
        value.apply {
            value.doSomethingElse("YAYA", "AHAH", "!!!!", "??")
        }

        for (i in 1..10) {
            value.doSomethingElse("$i")
        }

        println(value)
        value.doRealThing()

        println(value.realStringValue)

        try {
            doSomethingWithAValue(value)
        } catch (e: RealException) {
            println(e)
        }

        println(realName)
        println((this as RealSuperType).realName)
    }

    fun doSomethingWithAValue(cls: RealClass) {
        println(cls::class.java.name)

        throw RealException("Idk, i just wanted to throw this")
    }

    override val realName: String
        get() = "Real?"
}