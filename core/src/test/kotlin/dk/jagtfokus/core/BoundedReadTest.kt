package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class BoundedReadTest {
    @Test
    fun readsABodyAtTheLimit() {
        val bytes = ByteArray(100) { it.toByte() }
        assertContentEquals(bytes, bytes.inputStream().readBounded(100))
    }

    @Test
    fun refusesABodyOverTheLimit() {
        assertFailsWith<BodyTooLarge> { ByteArray(101).inputStream().readBounded(100) }
    }
}
