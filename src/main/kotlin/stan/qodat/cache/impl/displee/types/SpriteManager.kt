package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import org.slf4j.LoggerFactory
import qodat.cache.definition.SpriteDefinition
import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition as OsrsSpriteDefinition
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader
import java.util.Collections
import kotlin.system.measureTimeMillis

class SpriteManager(
    private val cacheLibrary: CacheLibrary
) {

    private val sprites: Multimap<Int, OsrsSpriteDefinition> = LinkedListMultimap.create()
    private val decodedArchives = HashSet<Int>()
    @Volatile
    private var listed = false

    @Synchronized
    fun load() {
        if (listed) return
        val index = cacheLibrary.index(8)
        val archiveIds = index.archiveIds()
        val elapsed = measureTimeMillis {
            for (archiveId in archiveIds) {
                sprites.put(archiveId, OsrsSpriteDefinition(archiveId, 0))
            }
        }
        listed = true
        logger.debug(
            "Sprite index-only list: {} ids in {}ms (no archive decompress)",
            archiveIds.size,
            elapsed,
        )
        if (sprites.isEmpty) {
            throw IllegalStateException("Sprite archive produced 0 sprites")
        }
    }

    fun getSprites(): Collection<SpriteDefinition> {
        load()
        return Collections.unmodifiableCollection(sprites.values())
    }

    fun findSprite(spriteId: Int, frame: Int): OsrsSpriteDefinition? {
        decodeArchive(spriteId)
        return sprites.get(spriteId).find { it.frame == frame }
    }

    fun getArchiveFrames(spriteId: Int): List<OsrsSpriteDefinition> {
        decodeArchive(spriteId)
        return sprites.get(spriteId).sortedBy { it.frame }
    }

    @Synchronized
    private fun decodeArchive(archiveId: Int) {
        load()
        if (!decodedArchives.add(archiveId)) return
        val archive = cacheLibrary.index(8).archive(archiveId) ?: return
        val data = archive.file(0)?.data ?: return
        val defs = SpriteLoader().load(archiveId, data)
        sprites.removeAll(archiveId)
        for (def in defs) {
            sprites.put(def.id, def)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpriteManager::class.java)

        internal fun indexEntries(archiveIds: IntArray): Array<OsrsSpriteDefinition> =
            Array(archiveIds.size) { OsrsSpriteDefinition(archiveIds[it], 0) }

        internal fun getSprites(sprites: Collection<OsrsSpriteDefinition>): Array<SpriteDefinition> =
            sprites.toTypedArray()
    }
}
