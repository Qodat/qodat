package stan.qodat.scene.runescape.entity

import qodat.cache.Cache
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup
import qodat.cache.definition.InterfaceDefinition
import qodat.cache.definition.ItemDefinition
import qodat.cache.definition.ModelDefinition
import qodat.cache.definition.NPCDefinition
import qodat.cache.definition.ObjectDefinition
import qodat.cache.definition.SpotAnimationDefinition
import qodat.cache.definition.SpriteDefinition
import qodat.cache.definition.TextureDefinition
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntityPrepareModelsTest {

    @Test
    fun prepareModelsDecodesOnceAndSkipsEmpty() {
        val cache = CountingCache()
        val item = Item(cache, FakeItemDefinition(arrayOf("10", "11")))
        item.prepareModels()
        item.prepareModels()
        assertEquals(2, cache.loads.get())
    }

    @Test
    fun prepareModelsRecordsEmptyWhenCacheHasNoModels() {
        val cache = CountingCache()
        val item = Item(cache, FakeItemDefinition(emptyArray()))
        item.prepareModels()
        assertEquals(0, cache.loads.get())
        assertTrue(item.getModels().isEmpty())
    }

    private class FakeItemDefinition(
        override val modelIds: Array<String>,
    ) : ItemDefinition {
        override val name = "Test item"
        override val findColor: ShortArray? = null
        override val replaceColor: ShortArray? = null
    }

    private class CountingCache : Cache("test") {
        val loads = AtomicInteger()
        override fun getModelDefinition(id: String): ModelDefinition {
            loads.incrementAndGet()
            return TinyModel(id)
        }
        override fun getAnimation(id: String): AnimationDefinition = error("unused")
        override fun getNPCs(): Array<NPCDefinition> = emptyArray()
        override fun getObjects(): Array<ObjectDefinition> = emptyArray()
        override fun getItems(): Array<ItemDefinition> = emptyArray()
        override fun getSpotAnimations(): Array<SpotAnimationDefinition> = emptyArray()
        override fun getAnimationDefinitions(): Array<AnimationDefinition> = emptyArray()
        override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup = error("unused")
        override fun getFrameDefinition(frameHash: Int): AnimationFrameLegacyDefinition? = null
        override fun getInterface(groupId: Int): Array<InterfaceDefinition> = emptyArray()
        override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> = emptyMap()
        override fun getSprites(): Array<SpriteDefinition> = emptyArray()
        override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition = error("unused")
        override fun getTexture(id: Int): TextureDefinition = error("unused")
    }

    private class TinyModel(private val name: String) : ModelDefinition {
        override fun getName() = name
        override fun getVertexCount() = 3
        override fun getVertexPositionsX() = intArrayOf(0, 1, 0)
        override fun getVertexPositionsY() = intArrayOf(0, 0, 1)
        override fun getVertexPositionsZ() = intArrayOf(0, 0, 0)
        override fun getVertexSkins(): IntArray? = null
        override fun getVertexGroups(): Array<IntArray>? = null
        override fun getVertexNormals(): Array<VertexNormal>? = null
        override fun getFaceCount() = 1
        override fun getFaceVertexIndices1() = intArrayOf(0)
        override fun getFaceVertexIndices2() = intArrayOf(1)
        override fun getFaceVertexIndices3() = intArrayOf(2)
        override fun getFaceSkins(): IntArray? = null
        override fun getFaceGroups(): Array<IntArray>? = null
        override fun getFaceColors() = shortArrayOf(100)
        override fun getFaceAlphas(): ByteArray? = null
        override fun getFacePriorities(): ByteArray? = null
        override fun getFaceTypes(): ByteArray? = null
        override fun getFaceNormals(): Array<FaceNormal?>? = null
        override fun getPriority() = 0.toByte()
        override fun getTextureConfigCount() = 0
        override fun getTextureRenderTypes(): ByteArray? = null
        override fun getFaceTextures(): ShortArray? = null
        override fun getFaceTextureConfigs(): ByteArray? = null
        override fun getTextureTriangleVertexIndices1(): ShortArray? = null
        override fun getTextureTriangleVertexIndices2(): ShortArray? = null
        override fun getTextureTriangleVertexIndices3(): ShortArray? = null
        override fun getFaceTextureUCoordinates(): Array<FloatArray>? = null
        override fun getFaceTextureVCoordinates(): Array<FloatArray>? = null
        override fun computeAnimationTables() {}
        override fun computeTextureUVCoordinates() {}
        override fun computeNormals() {}
    }
}
