package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyObjectDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyObjectDecoder
import java.nio.file.Files
import java.nio.file.Path

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/12/2019
 * @version 1.0
 */
object LegacyObjectStorage {

    private var cacheIndex = 0
    var objectCount = 0
    private lateinit var dataStream: InputStream
    private lateinit var dataStreamPositions: IntArray

    fun load(cachePath: Path) {

        val indexStream = InputStream(Files.readAllBytes(cachePath.resolve("loc.idx")))
        dataStream = InputStream(Files.readAllBytes(cachePath.resolve("loc.dat")))

        objectCount = indexStream.readUnsignedShort()
        dataStreamPositions = IntArray(objectCount)

        var offset = 2
        for (i in 0 until objectCount) {
            dataStreamPositions[i] = offset
            offset += indexStream.readUnsignedShort()
        }
        println("LegacyObjectStorage: loaded $objectCount objects")
    }

    operator fun get(id: Int): LegacyObjectDefinition? {

        dataStream.offset = dataStreamPositions[id]

        val decoder = LegacyObjectDecoder()
        return try {
            println("get $id ${dataStream.length} ${dataStream.offset}")
            decoder.load(
                id,
                dataStream
            )
        } catch (e: Exception){
            println(e.message)
            e.printStackTrace()
            null
        }
    }
}