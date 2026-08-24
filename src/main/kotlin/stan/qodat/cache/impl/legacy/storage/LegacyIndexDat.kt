package stan.qodat.cache.impl.legacy.storage

import com.displee.io.impl.InputBuffer
import java.nio.file.Files
import java.nio.file.Path

internal class LegacyIndexDat(
    cachePath: Path,
    indexFileName: String,
    datFileName: String,
) {
    val count: Int
    val dataStream: InputBuffer
    private val positions: IntArray

    init {
        val indexStream = InputBuffer(Files.readAllBytes(cachePath.resolve(indexFileName)))
        dataStream = InputBuffer(Files.readAllBytes(cachePath.resolve(datFileName)))
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
