package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import qodat.cache.definition.TextureDefinition
import stan.qodat.cache.impl.oldschool.definition.TextureDefinition as OsrsTextureDefinition
import stan.qodat.cache.impl.oldschool.loader.TextureLoader

class TextureManager(
    private val cacheLibrary: CacheLibrary
) {

    val textures = mutableMapOf<Int, OsrsTextureDefinition>()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(9).archive(0) ?: error("Texture archive not found")
        val loader = TextureLoader()
        archive.files.forEach { (fileId, file) ->
            val data = file.data ?: return@forEach
            textures[fileId] = loader.load(fileId, data)
        }
        loaded = true
    }

    fun findTexture(id: Int): OsrsTextureDefinition? {
        load()
        return textures[id]
    }

    fun getTextures(): Array<TextureDefinition> {
        load()
        return getTextures(textures)
    }

    companion object {
        internal fun getTextures(textures: Map<Int, OsrsTextureDefinition>): Array<TextureDefinition> =
            textures.values.toTypedArray()
    }
}
