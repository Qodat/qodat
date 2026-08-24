package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import qodat.cache.definition.SpotAnimationDefinition
import stan.qodat.cache.impl.oldschool.loader.SpotAnimLoader

class SpotAnimManager(
    private val cacheLibrary: CacheLibrary
) {
    private val spotAnims = mutableMapOf<Int, SpotAnimationDefinition>()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(13) ?: error("SpotAnim archive not found")
        val loader = SpotAnimLoader()
        archive.files.forEach { (fileId, file) ->
            spotAnims[fileId] = loader.load(fileId, file.data ?: error("SpotAnim data null"))
        }
        loaded = true
    }

    fun getSpotAnimations(): Array<SpotAnimationDefinition> {
        load()
        return getSpotAnimations(spotAnims)
    }

    companion object {
        internal fun getSpotAnimations(spotAnims: Map<Int, SpotAnimationDefinition>): Array<SpotAnimationDefinition> =
            spotAnims.values.toTypedArray()
    }
}
