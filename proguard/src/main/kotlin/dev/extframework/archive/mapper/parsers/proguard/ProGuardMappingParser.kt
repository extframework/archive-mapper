package dev.extframework.archive.mapper.parsers.proguard

import dev.extframework.archive.mapper.*
import dev.extframework.common.util.readInputStream
import org.objectweb.asm.Type
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

private const val CLASS_REGEX = """^(\S+) -> (\S+):$"""
private const val METHOD_REGEX =
    """^((?<from>\d+):(?<to>\d+):)?(?<ret>[^:]+)\s(?<name>[^:]+)\((?<args>.*)\)((:(?<obfFrom>\d+))?(:(?<obfTo>\d+))?)?\s->\s(?<obf>[^:]+)"""
private const val FIELD_REGEX = """^(\S+) (\S+) -> (\S+)$"""

public class ProGuardMappingParser(
    private val obfuscated: String,
    private val deObfuscated: String
) : MappingParser {
    private val classMatcher = Regex(CLASS_REGEX)
    private val methodMatcher = Regex(METHOD_REGEX)
    private val fieldMatcher = Regex(FIELD_REGEX)

    private val namespaces = setOf(obfuscated, deObfuscated)

    override val name: String = "proguard"

    private data class CurrentClass(
        val realName: String,
        val fakeName: String
    )

    override fun parse(mappingsIn: InputStream): ArchiveMapping {
        val bytes = mappingsIn.readInputStream()

        // DeObfuscated to obfuscated
        val typeMappings = HashMap<String, String>()

        // Creating an index of real to fake mappings to use for type conversions
        BufferedReader(
            InputStreamReader(
                ByteArrayInputStream(bytes)
            )
        ).use { reader ->
            var rawLine = reader.readLine()
            while (reader.ready()) {
                val gv = classMatcher.matchEntire(rawLine.trim())?.groupValues

                // Read a new line so we aren't stuck reading the same line forever.
                rawLine = reader.readLine()

                if (gv != null) {
                    val (_, deObfName, obfName) = gv

                    typeMappings[deObfName.replace('.', '/')] = obfName.replace('.', '/')
                }
            }
        }

        BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use { reader ->
            // List of classes in archive.
            val classes = HashSet<ClassMapping>()

            // If there are no lines to read return an empty mapped archive.
            if (!reader.ready()) return ArchiveMapping(
                setOf(),
                MappingValueContainerImpl(mapOf()),
                MappingNodeContainerImpl(setOf())
            )

            var line: String

            var currClass: CurrentClass? = null

            var methods = HashSet<MethodMapping>()
            var fields = HashSet<FieldMapping>()

            // Start reading lines
            while (true) {
                line = reader.readLine()?.trim() ?: break

                if (classMatcher.matches(line)) {
                    val (_, real, fake) = classMatcher.matchEntire(line)!!.groupValues

                    if (currClass != null) {
                        classes.add(parseClass(currClass, methods, fields))
                    }

                    currClass = CurrentClass(real, fake)
                    methods = HashSet()
                    fields = HashSet()
                } else if (methodMatcher.matches(line)) {
                    methods.add(parseMethod(line, typeMappings))
                }
                // Else, if the current line is a field
                else if (fieldMatcher.matches(line)) {
                    // Again, match and get group values

                    fields.add(parseField(line, typeMappings))
                }
                // Else, if the current line is source file attribute
                else if (line.startsWith("#")) {
                    continue
                }
            }

            if (currClass != null) classes.add(
                parseClass(
                    currClass, methods, fields
                )
            )

            // All classes read, can create a mapped archive and return.
            return ArchiveMapping(
                namespaces,
                MappingValueContainerImpl(mapOf()),
                classes.toMappingNodeContainer()
            )
        }
    }

    private fun <I : MappingIdentifier, T : MappingNode<I>> Set<T>.toMappingNodeContainer(): MappingNodeContainer<I, T> {
        return MappingNodeContainerImpl(this)
    }

    private fun parseClass(
        currClass: CurrentClass,
        methods: Set<MethodMapping>,
        fields: Set<FieldMapping>
    ) = ClassMapping(
        namespaces,
        MappingValueContainerImpl(
            mapOf(
                deObfuscated to ClassIdentifier(
                    currClass.realName.replace('.', '/'),
                    deObfuscated
                ),
                obfuscated to ClassIdentifier(
                    currClass.fakeName.replace('.', '/'),
                    obfuscated,
                )
            )
        ),
        methods.toMappingNodeContainer(),
        fields.toMappingNodeContainer()
    )

    private fun obfuscatedIdentifier(type: Type, typeMappings: Map<String, String>): Type {
        if (type.sort == Type.ARRAY) return Type.getType(
            "[" + obfuscatedIdentifier(
                Type.getType(
                    type.descriptor.removePrefix(
                        "["
                    ),
                ),
                typeMappings
            )
        )

        if (type.sort != Type.OBJECT) return type
        val fullQualifier = typeMappings[type.internalName]

        return if (fullQualifier != null) classToType(
            fullQualifier
        ) else type
    }

    private fun parseField(
        line: String,
        typeMappings: MutableMap<String, String>
    ): FieldMapping {
        val result = fieldMatcher.matchEntire(line)!!.groupValues

        // Create a mapped field and add to the fields list
        val realType = toTypeIdentifier(result[1])
        return FieldMapping(
            namespaces,

            // TODO replace with named regex params
            MappingValueContainerImpl(
                mapOf(
                    deObfuscated to FieldIdentifier(
                        result[2],
                        deObfuscated
                    ),
                    obfuscated to FieldIdentifier(
                        result[3],
                        obfuscated
                    )
                )
            ),
            MappingValueContainerImpl(
                mapOf(
                    deObfuscated to realType,
                    obfuscated to obfuscatedIdentifier(realType, typeMappings)
                )
            )
        )
    }

    private fun parseMethod(
        line: String,
        typeMappings: MutableMap<String, String>
    ): MethodMapping {
        val result = methodMatcher.matchEntire(line)!!.groups

        val realParameters: List<Type> =
            if (result["args"]?.value.isNullOrEmpty()) emptyList() else result["args"]!!.value.split(
                ','
            ).map(::toTypeIdentifier)

        // Read the groups, map the types, and add a method node to the methods
        val realReturnType = toTypeIdentifier(result["ret"]!!.value)

        return MethodMapping(
            // Namespaces
            namespaces,
            // Identifiers
            MappingValueContainerImpl(
                mapOf(
                    deObfuscated to MethodIdentifier(
                        result["name"]!!.value,
                        realParameters,
                        deObfuscated
                    ),
                    obfuscated to MethodIdentifier(
                        result["obf"]!!.value,
                        realParameters.map { obfuscatedIdentifier(it, typeMappings) },
                        obfuscated
                    )
                )
            ),
            // Line numbers
            lnStart = run {
                val deFrom = result["from"]?.value?.toIntOrNull()
                val obfFrom = result["obfFrom"]?.value?.toIntOrNull()

                if (deFrom == null || obfFrom == null) return@run null

                MappingValueContainerImpl(
                    mapOf(
                        deObfuscated to deFrom,
                        obfuscated to obfFrom
                    )
                )
            },
            lnEnd = run {
                val deTo = result["to"]?.value?.toIntOrNull()
                val obfTo = result["obfTo"]?.value?.toIntOrNull()
                if (deTo == null || obfTo == null) return@run null // Not clever, but is nice for the compiler

                MappingValueContainerImpl(
                    mapOf(
                        deObfuscated to deTo,
                        obfuscated to obfTo
                    )
                )
            },
            // Parameters
            returnType = MappingValueContainerImpl(
                mapOf(
                    deObfuscated to realReturnType,
                    obfuscated to obfuscatedIdentifier(realReturnType, typeMappings)
                )
            ),
        )
    }

    private fun toTypeIdentifier(desc: String): Type = when (desc) {
        "boolean" -> Type.BOOLEAN_TYPE
        "char" -> Type.CHAR_TYPE
        "byte" -> Type.BYTE_TYPE
        "short" -> Type.SHORT_TYPE
        "int" -> Type.INT_TYPE
        "float" -> Type.FLOAT_TYPE
        "long" -> Type.LONG_TYPE
        "double" -> Type.DOUBLE_TYPE
        "void" -> Type.VOID_TYPE
        else -> {
            if (desc.endsWith("[]")) {
                val type = desc.removeSuffix("[]")

                Type.getType("[" + toTypeIdentifier(type).descriptor)
            } else classToType(desc.replace('.', '/'))
        }
    }
}

private fun classToType(cls: String): Type = Type.getType("L$cls;")