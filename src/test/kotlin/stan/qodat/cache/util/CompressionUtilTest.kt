package stan.qodat.cache.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

class CompressionUtilTest {

    @Test
    fun uncompressGzipRestoresPayload() {
        val payload = "qodat-gzip-roundtrip".toByteArray()
        val compressed = ByteArrayOutputStream().use { bytes ->
            GZIPOutputStream(bytes).use { it.write(payload) }
            bytes.toByteArray()
        }
        assertTrue(CompressionUtil.uncompressGzip(compressed).contentEquals(payload))
    }
}
