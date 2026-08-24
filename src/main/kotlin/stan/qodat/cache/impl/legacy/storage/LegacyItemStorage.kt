package stan.qodat.cache.impl.legacy.storage

import stan.qodat.cache.impl.legacy.LegacyItemDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyItemDecoder
import java.nio.file.Path

object LegacyItemStorage {

    var itemCount = 0
    private lateinit var table: LegacyIndexDat

    fun load(cachePath: Path) {
        table = LegacyIndexDat(cachePath, "obj.idx", "objdat")
        itemCount = table.count
    }

    operator fun get(id: Int): LegacyItemDefinition? {
        table.seek(id)
        return LegacyItemDecoder().load(id, table.dataStream)
    }
}
