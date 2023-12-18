package net.yakclient.archive.mapper.parsers

import net.yakclient.archive.mapper.*
import net.yakclient.common.util.readInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

private const val CLASS_REGEX = """^(\S+) -> (\S+):$"""
private const val METHOD_REGEX =
    """^((?<from>\d+):(?<to>\d+):)?(?<ret>[^:]+)\s(?<name>[^:]+)\((?<args>.*)\)((:(?<originalFrom>\d+))?(:(?<originalTo>\d+))?)?\s->\s(?<obf>[^:]+)"""
private const val FIELD_REGEX = """^(\S+) (\S+) -> (\S+)$"""

public object ProGuardMappingParser : MappingParser {
    private val classMatcher = Regex(CLASS_REGEX)
    private val methodMatcher = Regex(METHOD_REGEX)
    private val fieldMatcher = Regex(FIELD_REGEX)

    override val name: String = "proguard"

    override fun parse(mappingsIn: InputStream): ArchiveMapping {
        val bytes = mappingsIn.readInputStream()

        // Real to fake
        val typeMappings = HashMap<String, String>()

        // Creating an index of real to fake mappings to use for type conversions
        BufferedReader(InputStreamReader(
            ByteArrayInputStream(bytes)
        )).use { reader ->
            var rawLine = reader.readLine()
            while (reader.ready()) {
                val gv = classMatcher.matchEntire(rawLine.trim())?.groupValues

                // Read a new line so we aren't stuck reading the same line forever.
                rawLine = reader.readLine()

                if (gv != null) {
                    val (_, realName, fakeName) = gv

                    typeMappings[realName.replace('.', '/')] = fakeName.replace('.', '/')
                }
            }
        }

        BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use { reader ->
            // Converts a list to an ObfuscationMap
            fun <I : MappingIdentifier, T : MappingNode<I>> List<T>.toBiMap(): Map<I, T> {
                val realMap = associateBy { it.realIdentifier }
                val fakeMap = associateBy { it.fakeIdentifier }

                return realMap + fakeMap
            }

            // List of classes in archive.
            val classes = ArrayList<ClassMapping>()

            // If there are no lines to read return an empty mapped archive.
            if (!reader.ready()) return ArchiveMapping(HashMap())

            // Read the first line
            var line: String = reader.readLine().trim()
            var oneMore = true // To make sure we get the last line

            // Start reading lines
            while (reader.ready() || oneMore.also { oneMore = false }) {
                // Match for a class
                val gv = classMatcher.matchEntire(line)?.groupValues

                // Check if the result is null, if so read the next line and continue.
                if (gv == null) {
                    // Read a new line so we aren't stuck reading the same line forever.
                    line = reader.readLine().trim()

                    continue
                }

                // Now that we have a class, get the real and fake names
                val (_, realName, fakeName) = gv

                // List of methods in class
                val methods = ArrayList<MethodMapping>()
                // List of fields in class
                val fields = ArrayList<FieldMapping>()

                // Start reading lines
                innerreader@ while (reader.ready()) {
                    // Update the line
                    line = reader.readLine().trim() // Read a new line

                    fun fakeIdentifier(type: TypeIdentifier): TypeIdentifier {
                        if (type is WrappedTypeIdentifier) return type.withNew(fakeIdentifier(type.innerType))

                        if (type !is ClassTypeIdentifier) return type
                        val fullQualifier = typeMappings[type.fullQualifier]

                        return if (fullQualifier != null) ClassTypeIdentifier(
                            fullQualifier
                        ) else type
                    }

                    // Check if the current line matched is a method definition
                    if (methodMatcher.matches(line)) {
                        // Get the result of this line, we know the match is not null due to the check above
                        val result = methodMatcher.matchEntire(line)!!.groups as MatchNamedGroupCollection

                        val realParameters: List<TypeIdentifier> =
                            if (result["args"]?.value.isNullOrEmpty()) emptyList() else result["args"]!!.value.split(
                                ','
                            ).map(::toTypeIdentifier)

                        // Read the groups, map the types, and add a method node to the methods
                        val realReturnType = toTypeIdentifier(result["ret"]!!.value)

                        methods.add(
                            MethodMapping(
                                realIdentifier = MethodIdentifier(
                                    result["name"]!!.value,
                                    realParameters,
                                    MappingType.REAL
                                ), // Real name
                                fakeIdentifier = MethodIdentifier(
                                    result["obf"]!!.value,
                                    realParameters.map(::fakeIdentifier),
                                    MappingType.FAKE
                                ), // Fake name
                                lnStart = result["from"]?.value?.toIntOrNull(), // Start line
                                lnEnd = result["to"]?.value?.toIntOrNull(), // End line
                                originalLnStart = result["originalFrom"]?.value?.toIntOrNull(), // Original start line
                                originalLnEnd = result["originalTo"]?.value?.toIntOrNull(), // Original end line
                                // Parameters
                                realReturnType = realReturnType, // Return type
                                fakeReturnType = fakeIdentifier(realReturnType)
                            )
                        )
                    }
                    // Else, if the current line is a field
                    else if (fieldMatcher.matches(line)) {
                        // Again, match and get group values
                        val result = fieldMatcher.matchEntire(line)!!.groupValues

                        // Create a mapped field and add to the fields list
                        val realType = toTypeIdentifier(result[1])
                        fields.add(
                            FieldMapping(
                                FieldIdentifier(
                                    result[2],
                                    MappingType.REAL
                                ),
                                FieldIdentifier(
                                    result[3],
                                    MappingType.FAKE
                                ), // Fake name
                                realType, // Type
                                fakeIdentifier(realType)
                            )
                        )
                    }
                    // Else, if the current line is source file attribute
                    else if (line.startsWith("#")) {
                        continue@innerreader
                    }
                    // If its not a field or method, then the class definition is over, and we can break.
                    else break@innerreader
                }

                // Method and field reading is done, create a mapped class and add it to the classes list.
                classes.add(
                    ClassMapping(
                        ClassIdentifier(
                            realName.replace('.', '/'),
                            MappingType.REAL
                        ),
                        ClassIdentifier(
                            fakeName.replace('.', '/'),
                            MappingType.FAKE,
                        ),
                        methods.toBiMap(),
                        fields.toBiMap()
                    )
                )
            }

            // All classes read, can create a mapped archive and return.
            return ArchiveMapping(classes.toBiMap())
        }
    }

    private fun toTypeIdentifier(desc: String): TypeIdentifier = when (desc) {
        "boolean" -> PrimitiveTypeIdentifier.BOOLEAN
        "char" -> PrimitiveTypeIdentifier.CHAR
        "byte" -> PrimitiveTypeIdentifier.BYTE
        "short" -> PrimitiveTypeIdentifier.SHORT
        "int" -> PrimitiveTypeIdentifier.INT
        "float" -> PrimitiveTypeIdentifier.FLOAT
        "long" -> PrimitiveTypeIdentifier.LONG
        "double" -> PrimitiveTypeIdentifier.DOUBLE
        "void" -> PrimitiveTypeIdentifier.VOID
        else -> {
            if (desc.endsWith("[]")) {
                val type = desc.removeSuffix("[]")

                ArrayTypeIdentifier(toTypeIdentifier(type))
            } else ClassTypeIdentifier(desc.replace('.', '/'))
        }
    }
}


