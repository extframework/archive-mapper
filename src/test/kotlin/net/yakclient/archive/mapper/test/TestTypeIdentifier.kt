package net.yakclient.archive.mapper.test

import net.yakclient.archive.mapper.ArrayTypeIdentifier
import net.yakclient.archive.mapper.ClassTypeIdentifier
import net.yakclient.archive.mapper.PrimitiveTypeIdentifier
import net.yakclient.archive.mapper.fromInternalType
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