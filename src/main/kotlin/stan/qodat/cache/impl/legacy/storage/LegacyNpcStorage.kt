package stan.qodat.cache.impl.legacy.storage

import stan.qodat.cache.impl.legacy.LegacyNpcDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyNpcDecoder
import java.nio.file.Path

object LegacyNpcStorage {

    var npcCount = 0

    private lateinit var table: LegacyIndexDat

    fun load(cachePath: Path) {
        table = LegacyIndexDat(cachePath, "npc.idx", "npc.dat")
        npcCount = table.count
    }

    operator fun get(id: Int): LegacyNpcDefinition {
        // TODO(perf): constructs a new decoder per id; getNPCs() does this for every npc
        table.seek(id)
        return LegacyNpcDecoder().load(id, table.dataStream)
    }
}
