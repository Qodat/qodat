package stan.qodat.cache.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * A utility class for performing compression/decompression.
 *
 * @author Graham
 */
object CompressionUtil {

    /**
     * Degzips **all** of the datain the specified [ByteBuffer].
     *
     * @param compressed The compressed buffer.
     * @return The decompressed array.
     * @throws IOException If there is an error decompressing the buffer.
     */
    @Throws(IOException::class)
    fun compressGzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { gzip ->
                gzip.write(data)
            }
            out.toByteArray()
        }

    @Throws(IOException::class)
    fun uncompressGzip(compressed: ByteArray) = GZIPInputStream(ByteArrayInputStream(compressed)).use { `is` ->
            // TODO(perf): 1KB copy buffer is small for frame archives; consider 8–16KB or a reused Inflater
            ByteArrayOutputStream().use { out ->
                val buffer = ByteArray(1024)

                while (true) {
                    val read = `is`.read(buffer, 0, buffer.size)
                    if (read == -1) {
                        break
                    }

                    out.write(buffer, 0, read)
                }

                out.toByteArray()
            }
    }
}