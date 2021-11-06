package stan.qodat.cache.impl.legacy.storage

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyKitDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyKitDecoder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object LegacyBodyKitStorage {

    private lateinit var kits : Array<LegacyKitDefinition>

    @Throws(IOException::class)
    fun load(cachePath: Path) {

        val stream = InputStream(Files.readAllBytes(cachePath.resolve("idk.dat")))

        kits = Array(stream.readUnsignedShort()) {
            LegacyKitDecoder().load(it, stream)
        }
    }

    fun getKit(id: Int) = kits[id]

}
