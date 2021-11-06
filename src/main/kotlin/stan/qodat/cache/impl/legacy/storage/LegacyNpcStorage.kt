package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyNpcDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyNpcDecoder
import java.nio.file.Files
import java.nio.file.Path

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-11
 */
object LegacyNpcStorage {

    var npcCount = 0

    private var cacheIndex = 0
    private lateinit var dataStream: InputStream
    private lateinit var dataStreamPositions: IntArray

    fun load(cachePath: Path) {

        val indexStream = InputStream(Files.readAllBytes(cachePath.resolve("npc.idx")))
        dataStream = InputStream(Files.readAllBytes(cachePath.resolve("npc.dat")))

        npcCount = indexStream.readUnsignedShort()
        dataStreamPositions = IntArray(npcCount)

        var offset = 2
        for (i in 0 until npcCount) {
            dataStreamPositions[i] = offset
            offset += indexStream.readUnsignedShort()
        }
        println("LegacyNpcStorage: loaded $npcCount npcs")
    }


    operator fun get(id: Int): LegacyNpcDefinition {

        cacheIndex = (cacheIndex + 1) % 20
        dataStream.offset = dataStreamPositions[id]

        val decoder = LegacyNpcDecoder()
        return decoder.load(id, dataStream)
    }
}
