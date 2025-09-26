package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.SpotAnimDefinition
import net.runelite.cache.definitions.loaders.SpotAnimLoader
import qodat.cache.definition.SpotAnimationDefinition
import stan.qodat.scene.runescape.entity.SpotAnimation
import java.util.OptionalInt

class SpotAnimManager(
    private val cacheLibrary: CacheLibrary
) {
    private val spotAnims = mutableMapOf<Int, SpotAnimationDefinition>()

    fun load() {
        val loader = SpotAnimLoader()
        val archive = cacheLibrary.index(2).archive(13)?:error("SpotAnim archive not found")
        archive.files.forEach { (fileId, file) ->
            spotAnims.put(fileId, convert(loader.load(fileId, file.data)))
        }
    }

    private fun convert(spotAnim: SpotAnimDefinition): SpotAnimationDefinition {
        return object : SpotAnimationDefinition {
            override fun getOptionalId() = OptionalInt.of(spotAnim.id)
            override val name: String = spotAnim.id.toString()
            override val modelIds: Array<String> = arrayOf(spotAnim.getModelId().toString())
            override val findColor: ShortArray? = spotAnim.recolorToFind
            override val replaceColor: ShortArray? = spotAnim.recolorToReplace
            override val animationIds: Array<String> = arrayOf(spotAnim.animationId.toString())
        }
    }

    fun getSpotAnimations() = spotAnims.values.toTypedArray()

    fun getSpotAnimation(id: Int) = spotAnims[id]?: throw IllegalArgumentException("SpotAnimation $id not found")
}