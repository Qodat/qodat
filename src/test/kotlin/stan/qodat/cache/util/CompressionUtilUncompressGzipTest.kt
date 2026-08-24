package stan.qodat.cache.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompressionUtilUncompressGzipTest {

    @Test
    fun uncompressGzipRoundTripsEmptyAndBinaryPayloads() {
        val payloads = arrayOf(
            byteArrayOf(),
            byteArrayOf(0, 0, 0),
            byteArrayOf(1, 2, 3, 127, (-1).toByte()),
        )
        for (payload in payloads) {
            assertTrue(CompressionUtil.uncompressGzip(gzip(payload)).contentEquals(payload))
        }
    }

    @Test
    fun uncompressGzipRejectsInvalidInput() {
        assertFailsWith<IOException> { CompressionUtil.uncompressGzip(byteArrayOf()) }
        assertFailsWith<IOException> { CompressionUtil.uncompressGzip(byteArrayOf(0x1F, 0x8B.toByte(), 0x00)) }
        assertFailsWith<IOException> { CompressionUtil.uncompressGzip("not-gzip".toByteArray()) }
    }

    private fun gzip(payload: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { bytes ->
            GZIPOutputStream(bytes).use { it.write(payload) }
            bytes.toByteArray()
        }
    }
}
