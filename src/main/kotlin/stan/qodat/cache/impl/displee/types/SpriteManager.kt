package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import net.runelite.cache.definitions.SpriteDefinition
import net.runelite.cache.definitions.loaders.SpriteLoader
import stan.qodat.cache.CacheParallel
import java.util.Collections

class SpriteManager(
    private val cacheLibrary: CacheLibrary
) {

    private val sprites: Multimap<Int, SpriteDefinition> = LinkedListMultimap.create()
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

    fun findSprite(spriteId: Int, frame: Int): SpriteDefinition? {
        load()
        return sprites.get(spriteId).find { it.frame == frame }
    }

    private class SpriteArchivePayload(
        val archiveId: Int,
        val data: ByteArray
    )
}
