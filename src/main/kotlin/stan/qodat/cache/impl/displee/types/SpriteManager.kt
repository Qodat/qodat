package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import net.runelite.cache.definitions.SpriteDefinition
import net.runelite.cache.definitions.loaders.SpriteLoader
import net.runelite.cache.util.Djb2
import stan.qodat.cache.CacheParallel
import java.awt.image.BufferedImage
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class SpriteManager(
    private val cacheLibrary: CacheLibrary
) {

    private val sprites: Multimap<Int?, SpriteDefinition> = LinkedListMultimap.create<Int, SpriteDefinition>()
    private val spriteIdsByArchiveNameHash: MutableMap<Int, Int> = HashMap()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val index = cacheLibrary.index(8)
        val archiveIds = index.archiveIds()
        val decoded = ConcurrentHashMap<Int, Array<SpriteDefinition>>()
        val nameHashes = ConcurrentHashMap<Int, Int>()

        CacheParallel.forEachIndexed(archiveIds.size) { i ->
            val archiveId = archiveIds[i]
            val archive = index.archive(archiveId) ?: return@forEachIndexed
            val contents = archive.file(0) ?: return@forEachIndexed
            val data = contents.data ?: return@forEachIndexed
            val defs = SpriteLoader().load(archive.id, data)
            decoded[archiveId] = defs
            nameHashes[archiveId] = archive.hashName
        }

        for (archiveId in archiveIds) {
            val defs = decoded[archiveId] ?: continue
            val hashName = nameHashes[archiveId] ?: 0
            for (sprite in defs) {
                sprites.put(sprite.id, sprite)
                spriteIdsByArchiveNameHash[hashName] = sprite.id
            }
        }
        loaded = true
    }

    fun getSprites(): Collection<SpriteDefinition> {
        load()
        return Collections.unmodifiableCollection(sprites.values())
    }

    fun findSprite(spriteId: Int, frame: Int): SpriteDefinition? {
        load()
        return sprites.get(spriteId).find { it.frame == frame }
    }

    fun getSpriteImage(sprite: SpriteDefinition): BufferedImage {
        val image = BufferedImage(sprite.width, sprite.height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, sprite.width, sprite.height, sprite.pixels, 0, sprite.width)
        return image
    }

    fun findSpriteByArchiveName(name: String, frameId: Int): SpriteDefinition? {
        load()
        val nameHash = Djb2.hash(name)
        val spriteId = spriteIdsByArchiveNameHash[nameHash] ?: return null
        return findSprite(spriteId, frameId)
    }
}
