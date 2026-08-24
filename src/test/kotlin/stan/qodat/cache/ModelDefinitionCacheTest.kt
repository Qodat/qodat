package stan.qodat.cache

import qodat.cache.definition.ModelDefinition
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelDefinitionCacheTest {

    @AfterTest
    fun tearDown() {
        ModelDefinitionCache.clear()
    }

    @Test
    fun getOrLoadReturnsCachedDefinition() {
        val loads = AtomicInteger()
        val first = ModelDefinitionCache.getOrLoad("t", "1") {
            loads.incrementAndGet()
            TinyModel("1")
        }
        val second = ModelDefinitionCache.getOrLoad("t", "1") {
            loads.incrementAndGet()
            TinyModel("reload")
        }
        assertEquals(1, loads.get())
        assertTrue(first === second)
    }

    @Test
    fun evictsEldestWhenOverEntryCap() {
        ModelDefinitionCache.getOrLoad("t", "0") { TinyModel("0") }
        repeat(ModelDefinitionCache.MAX_ENTRIES) { index ->
            ModelDefinitionCache.getOrLoad("t", "${index + 1}") { TinyModel("${index + 1}") }
        }
        val reloads = AtomicInteger()
        ModelDefinitionCache.getOrLoad("t", "0") {
            reloads.incrementAndGet()
            TinyModel("0-again")
        }
        assertEquals(1, reloads.get())
    }

    @Test
    fun estimateBytesCountsVertexAndFaceBuffers() {
        val bytes = ModelDefinitionCache.estimateBytes(TinyModel("x"))
        assertTrue(bytes > 64L)
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
