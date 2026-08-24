package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import qodat.cache.definition.SpriteDefinition
import stan.qodat.cache.CacheParallel
import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition as OsrsSpriteDefinition
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader
import java.util.Collections

class SpriteManager(
    private val cacheLibrary: CacheLibrary
) {

    private val sprites: Multimap<Int, OsrsSpriteDefinition> = LinkedListMultimap.create()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val index = cacheLibrary.index(8)
        val archiveIds = index.archiveIds()
        val payloads = ArrayList<SpriteArchivePayload>(archiveIds.size)

        for (archiveId in archiveIds) {
            val archive = index.archive(archiveId) ?: continue
            val contents = archive.file(0) ?: continue
            val data = contents.data ?: continue
            payloads.add(SpriteArchivePayload(archive.id, data))
        }

        val decoded = CacheParallel.decode(payloads.map { it.archiveId to it }) { _, payload ->
            SpriteLoader().load(payload.archiveId, payload.data)
        }

        for (archiveId in archiveIds) {
            val defs = decoded[archiveId] ?: continue
            for (sprite in defs) {
                sprites.put(sprite.id, sprite)
            }
        }
        loaded = true
        if (sprites.isEmpty) {
            throw IllegalStateException("Sprite archive produced 0 sprites")
        }
    }

    fun getSprites(): Collection<SpriteDefinition> {
        load()
        return Collections.unmodifiableCollection(sprites.values())
    }

    fun findSprite(spriteId: Int, frame: Int): OsrsSpriteDefinition? {
        load()
        return sprites.get(spriteId).find { it.frame == frame }
    }

    companion object {
        internal fun getSprites(sprites: Collection<OsrsSpriteDefinition>): Array<SpriteDefinition> =
            sprites.toTypedArray()
    }

    private class SpriteArchivePayload(
        val archiveId: Int,
        val data: ByteArray
    )
}
