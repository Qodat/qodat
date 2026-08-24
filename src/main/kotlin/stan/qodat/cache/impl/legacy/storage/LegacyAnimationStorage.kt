package stan.qodat.cache.impl.legacy.storage

import com.displee.io.impl.InputBuffer
import stan.qodat.cache.impl.legacy.LegacyAnimationDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacySequenceDecoder

import java.nio.file.Files
import java.nio.file.Path

object LegacyAnimationStorage {

    lateinit var animations: Array<LegacyAnimationDefinition>

    fun load(cachePath: Path) {
        val stream = InputBuffer(Files.readAllBytes(cachePath.resolve("seq.dat")))
        val length = stream.readUnsignedShort()
        animations = Array(length) {
            LegacySequenceDecoder().load(it, stream)
        }
    }

    operator fun get(id: Int) = animations[id]
}
