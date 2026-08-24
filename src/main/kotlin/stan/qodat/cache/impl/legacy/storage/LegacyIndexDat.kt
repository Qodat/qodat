package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

internal class LegacyIndexDat(
    cachePath: Path,
    indexFileName: String,
    datFileName: String,
) {
    val count: Int
    val dataStream: InputStream
    private val positions: IntArray

    init {
        val indexStream = InputStream(Files.readAllBytes(cachePath.resolve(indexFileName)))
        dataStream = InputStream(Files.readAllBytes(cachePath.resolve(datFileName)))
        count = indexStream.readUnsignedShort()
        positions = IntArray(count)
        var offset = 2
        for (i in 0 until count) {
            positions[i] = offset
            offset += indexStream.readUnsignedShort()
        }
    }

    fun seek(id: Int) {
        dataStream.offset = positions[id]
    }
}
