package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyAnimationDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacySequenceDecoder

import java.nio.file.Files
import java.nio.file.Path

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-08
 */
object LegacyAnimationStorage {

    lateinit var animations: Array<LegacyAnimationDefinition>

    fun load(cachePath: Path) {

        val stream = InputStream(Files.readAllBytes(cachePath.resolve("seq.dat")))
        val length = stream.readUnsignedShort()

        animations = Array(length){
            LegacySequenceDecoder().load(it, stream)
        }

        println("LegacySequenceStorage: loaded $length animations.")
    }

    operator fun get(id: Int) = animations[id]

}
