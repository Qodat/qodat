package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import net.runelite.cache.definitions.SpriteDefinition
import net.runelite.cache.definitions.loaders.SpriteLoader
import net.runelite.cache.util.Djb2
import java.awt.image.BufferedImage
import java.util.Collections

class SpriteManager(
    private val cacheLibrary: CacheLibrary
) {

    private val sprites: Multimap<Int?, SpriteDefinition> = LinkedListMultimap.create<Int, SpriteDefinition>()
    private val spriteIdsByArchiveNameHash: MutableMap<Int, Int> = HashMap()

    fun load() {
        val index = cacheLibrary.index(8)
        for (archiveId in index.archiveIds()) {
            val archive = index.archive(archiveId) ?: error("Missing sprite archive: $archiveId")
            val contents = archive.file(0) ?: error("Missing sprite archive: ${archive.id}")
            val loader = SpriteLoader()
            val defs = loader.load(archive.id, contents.data)
            for (sprite in defs) {
                sprites.put(sprite.id, sprite)
                spriteIdsByArchiveNameHash[archive.hashName] = sprite.id
            }
        }
    }

    fun getSprites() =
        Collections.unmodifiableCollection(sprites.values())

    fun findSprite(spriteId: Int, frame: Int) =
        sprites.get(spriteId).find { it.frame == frame }

    fun getSpriteImage(sprite: SpriteDefinition): BufferedImage {
        val image = BufferedImage(sprite.width, sprite.height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, sprite.width, sprite.height, sprite.pixels, 0, sprite.width)
        return image
    }

    fun findSpriteByArchiveName(name: String, frameId: Int): SpriteDefinition? {
        val nameHash = Djb2.hash(name)
        val spriteId = spriteIdsByArchiveNameHash[nameHash] ?: return null
        return findSprite(spriteId, frameId)
    }
}