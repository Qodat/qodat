package qodat.cache.models

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class RS2ModelUvTest {

    @Test
    fun vCoordinatesAreIndependentOfU() {
        val model = RS2Model()
        model.setFaceCount(1)
        model.setFaceVertexIndices1(intArrayOf(0))
        model.setFaceVertexIndices2(intArrayOf(1))
        model.setFaceVertexIndices3(intArrayOf(2))
        model.setFaceTextures(shortArrayOf(40))
        model.setFaceTextureConfigs(byteArrayOf((-1).toByte()))

        model.computeTextureUVCoordinates()

        val u = model.getFaceTextureUCoordinates()
        val v = model.getFaceTextureVCoordinates()
        assertNotNull(u)
        assertNotNull(v)
        assertContentEquals(floatArrayOf(0f, 1f, 0f), u[0])
        assertContentEquals(floatArrayOf(1f, 1f, 0f), v[0])
        assertFalse(u[0].contentEquals(v[0]))
    }
}
