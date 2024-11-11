package dev.extframework.archive.mapper.parsers.mcp

internal data class RichMcpMapping(
    val type: Type,
    val key: String,
    val value: String,
    val comment: String?
) {
    enum class Type(val descriptor: String) {
        CLASS("class"),
        FIELD("field"),
        METHOD("method"),
        PARAMETER("parameter");

        companion object {
            fun fromDescriptor(descriptor: String): Type = when (descriptor) {
                "class" -> CLASS
                "field" -> FIELD
                "method" -> METHOD
                "parameter" -> PARAMETER
                else -> throw IllegalArgumentException("Unknown descriptor: $descriptor")
            }
        }
    }
}
