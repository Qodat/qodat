package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyItemDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyItemDecoder
import java.nio.file.Files
import java.nio.file.Path

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-11
 */
object LegacyItemStorage {

    private var cacheIndex = 0
    var itemCount = 0
    private lateinit var dataStream: InputStream
    private lateinit var dataStreamPositions: IntArray

    fun load(cachePath: Path) {

        val indexStream = InputStream(Files.readAllBytes(cachePath.resolve("obj.idx")))
        dataStream = InputStream(Files.readAllBytes(cachePath.resolve("objdat")))

        itemCount = indexStream.readUnsignedShort()
        dataStreamPositions = IntArray(itemCount)

        var offset = 2
        for (i in 0 until itemCount) {
            dataStreamPositions[i] = offset
            offset += indexStream.readUnsignedShort()
        }
        println("LegacyItemStorage: loaded $itemCount npcs")
    }


    operator fun get(id: Int): LegacyItemDefinition? {

        cacheIndex = (cacheIndex + 1) % 20
        dataStream.offset = dataStreamPositions[id]

        val decoder = LegacyItemDecoder()

        return decoder.load(id, dataStream)
    }

}
