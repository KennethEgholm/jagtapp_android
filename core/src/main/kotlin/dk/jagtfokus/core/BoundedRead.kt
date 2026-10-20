package dk.jagtfokus.core

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** A download that is larger than the cap the caller set. */
class BodyTooLarge : Exception()

/** Reads at most [limit] bytes. Throws [BodyTooLarge] as soon as one more byte shows up. */
fun InputStream.readBounded(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        total += n
        if (total > limit) throw BodyTooLarge()
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
