package dev.extframework.archive.mapper.test

import dev.extframework.archive.mapper.ArrayTypeIdentifier
import dev.extframework.archive.mapper.ClassTypeIdentifier
import dev.extframework.archive.mapper.PrimitiveTypeIdentifier
import dev.extframework.archive.mapper.fromInternalType
import kotlin.test.Test

class TestTypeIdentifier {
    @Test
    fun `Test type identifier parses internal type correctly`() {
        check(listOf(
            "B",
            "Z",
            "J",
            "net/asdf/Something",
            "[[[Something",
            "[net/yakclient/Hello",
            "[B",
            "[[[J"
        ).map { fromInternalType(it) } == listOf(
            PrimitiveTypeIdentifier.BYTE,
            PrimitiveTypeIdentifier.BOOLEAN,
            PrimitiveTypeIdentifier.LONG,
            ClassTypeIdentifier("net/asdf/Something"),
            ArrayTypeIdentifier(
                ArrayTypeIdentifier(
                    ArrayTypeIdentifier(
                        ClassTypeIdentifier("Something")
                    )
                )
            ),
            ArrayTypeIdentifier(
                ClassTypeIdentifier("net/yakclient/Hello")
            ),
            ArrayTypeIdentifier(
                PrimitiveTypeIdentifier.BOOLEAN
            ),
            ArrayTypeIdentifier(
                ArrayTypeIdentifier(
                    ArrayTypeIdentifier(
                        PrimitiveTypeIdentifier.DOUBLE
                    )
                )
            )
        ))
    }
}