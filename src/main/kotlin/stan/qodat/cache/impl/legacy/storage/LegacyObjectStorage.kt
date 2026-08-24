package stan.qodat.cache.impl.legacy.storage

import stan.qodat.cache.impl.legacy.LegacyObjectDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyObjectDecoder
import java.nio.file.Path

object LegacyObjectStorage {

    var objectCount = 0
    private lateinit var table: LegacyIndexDat

    fun load(cachePath: Path) {
        table = LegacyIndexDat(cachePath, "loc.idx", "loc.dat")
        objectCount = table.count
    }

    operator fun get(id: Int): LegacyObjectDefinition? {
        table.seek(id)
        return try {
            LegacyObjectDecoder().load(id, table.dataStream)
        } catch (_: Exception) {
            null
        }
    }
}
