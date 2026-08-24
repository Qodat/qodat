package stan.qodat.scene.runescape

import qodat.cache.definition.ModelDefinition
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuneScapeRenderingTest {

    @Test
    fun leftoverAccessorsReadFaceVerticesAndPositions() {
        val model = FakeModelDefinition(
            xs = intArrayOf(1, 2, 3),
            ys = intArrayOf(4, 5, 6),
            zs = intArrayOf(7, 8, 9),
            i1 = intArrayOf(2),
            i2 = intArrayOf(0),
            i3 = intArrayOf(1),
            colors = shortArrayOf(10)
        )
        assertEquals(Triple(2, 0, 1), model.getVertices(0))
        assertEquals(1, model.getX(0))
        assertEquals(5, model.getY(1))
        assertEquals(9, model.getZ(2))
    }

    @Test
    fun isFaceHiddenTreatsAlphaMinusOneAndUnknownTypesAsHidden() {
        val hiddenAlpha = triangle(alphas = byteArrayOf((-1).toByte()))
        val unlitAlpha = triangle(alphas = byteArrayOf((-2).toByte()), types = byteArrayOf(RENDER_HIDDEN_TRIANGLE.toByte()))
        val gouraud = triangle(types = byteArrayOf(RENDER_GOURAUD_TRIANGLE.toByte()))
        val flat = triangle(types = byteArrayOf(RENDER_FLAT_TRIANGLE.toByte()))
        val unlit = triangle(types = byteArrayOf(RENDER_UNLIT_TRIANGLE.toByte()))
        val hiddenType = triangle(types = byteArrayOf(RENDER_HIDDEN_TRIANGLE.toByte()))
        val otherType = triangle(types = byteArrayOf(4))
        val defaultType = triangle()

        assertTrue(hiddenAlpha.isFaceHidden(0))
        assertFalse(unlitAlpha.isFaceHidden(0))
        assertFalse(gouraud.isFaceHidden(0))
        assertFalse(flat.isFaceHidden(0))
        assertFalse(unlit.isFaceHidden(0))
        assertTrue(hiddenType.isFaceHidden(0))
        assertTrue(otherType.isFaceHidden(0))
        assertFalse(defaultType.isFaceHidden(0))
    }

    @Test
    fun faceShadingIsFlatWhenAllCornersMatch() {
        val flat = FaceShading(intArrayOf(10, 11), intArrayOf(10, 12), intArrayOf(10, 11))
        assertTrue(flat.isFlat(0))
        assertFalse(flat.isFlat(1))
    }

    @Test
    fun lightLeavesUnlitFacesAtFixedColourAndHiddenFacesUnshaded() {
        val color = 0x1A40
        val unlit = triangle(colors = shortArrayOf(color.toShort()), types = byteArrayOf(RENDER_UNLIT_TRIANGLE.toByte()))
        val hidden = triangle(colors = shortArrayOf(color.toShort()), types = byteArrayOf(RENDER_HIDDEN_TRIANGLE.toByte()))
        val alphaUnlit = triangle(colors = shortArrayOf(color.toShort()), alphas = byteArrayOf((-2).toByte()))
        val alphaHidden = triangle(colors = shortArrayOf(color.toShort()), alphas = byteArrayOf((-1).toByte()))

        val unlitShading = unlit.light()
        assertEquals(128, unlitShading.corner1[0])
        assertEquals(128, unlitShading.corner2[0])
        assertEquals(128, unlitShading.corner3[0])
        assertTrue(unlitShading.isFlat(0))

        val hiddenShading = hidden.light()
        assertEquals(color, hiddenShading.corner1[0])
        assertEquals(color, hiddenShading.corner2[0])
        assertEquals(color, hiddenShading.corner3[0])

        assertEquals(128, alphaUnlit.light().corner1[0])
        assertEquals(color, alphaHidden.light().corner1[0])
    }

    @Test
    fun lightShadesFlatFacesUniformlyAndKeepsHueSaturation() {
        val color = 0x2A55
        val model = triangle(colors = shortArrayOf(color.toShort()), types = byteArrayOf(RENDER_FLAT_TRIANGLE.toByte()))
        val shading = model.light()

        assertTrue(shading.isFlat(0))
        assertEquals(color and 65408, shading.corner1[0] and 65408)
        assertTrue(shading.corner1[0] and 127 in 2..126)
    }

    @Test
    fun gouraudLightVariesWithSharedVertexNormals() {
        val color = 0x1840
        val model = FakeModelDefinition(
            xs = intArrayOf(0, 100, 0, 0),
            ys = intArrayOf(0, 0, 100, 0),
            zs = intArrayOf(0, 0, 0, 100),
            i1 = intArrayOf(0, 0),
            i2 = intArrayOf(1, 2),
            i3 = intArrayOf(2, 3),
            colors = shortArrayOf(color.toShort(), color.toShort())
        )
        val shading = model.light(lightX = 0, lightY = 0, lightZ = -50)
        val corners = intArrayOf(shading.corner1[0], shading.corner2[0], shading.corner3[0])
        assertTrue(corners.distinct().size > 1)
        for (packed in corners) {
            assertEquals(color and 65408, packed and 65408)
            assertTrue(packed and 127 in 2..126)
        }
    }

    @Test
    fun lightAcceptsZeroVectorAndFaceColourOverride() {
        val model = triangle(colors = shortArrayOf(20))
        val shading = model.light(lightX = 0, lightY = 0, lightZ = 0, faceColors = intArrayOf(0x103F))
        assertEquals(0x1000, shading.corner1[0] and 65408)
        assertTrue(shading.corner1[0] and 127 in 2..126)
    }

    @Test
    fun actorAndSceneryDefaultsProduceDifferentBrightness() {
        val model = triangle(colors = shortArrayOf(0x2040), types = byteArrayOf(RENDER_FLAT_TRIANGLE.toByte()))
        val actor = model.light(ACTOR_AMBIENT, ACTOR_CONTRAST, ACTOR_LIGHT_X, ACTOR_LIGHT_Y, ACTOR_LIGHT_Z)
        val scenery = model.light(SCENERY_AMBIENT, SCENERY_CONTRAST, SCENERY_LIGHT_X, SCENERY_LIGHT_Y, SCENERY_LIGHT_Z)
        assertTrue(actor.corner1[0] != scenery.corner1[0])
    }

    @Test
    fun lightingConstantsMatchClientValues() {
        assertEquals(8192, MAX_VIEW_DISTANCE)
        assertEquals(0, RENDER_GOURAUD_TRIANGLE)
        assertEquals(1, RENDER_FLAT_TRIANGLE)
        assertEquals(2, RENDER_HIDDEN_TRIANGLE)
        assertEquals(3, RENDER_UNLIT_TRIANGLE)
        assertEquals(64, ACTOR_AMBIENT)
        assertEquals(64, SCENERY_AMBIENT)
        assertEquals(850, ACTOR_CONTRAST)
        assertEquals(768, SCENERY_CONTRAST)
        assertEquals(-50, ACTOR_LIGHT_Y)
        assertEquals(-10, SCENERY_LIGHT_Y)
    }

    private fun triangle(
        colors: ShortArray = shortArrayOf(128),
        types: ByteArray? = null,
        alphas: ByteArray? = null
    ) = FakeModelDefinition(
        xs = intArrayOf(0, 128, 0),
        ys = intArrayOf(0, 0, 128),
        zs = intArrayOf(0, 0, 0),
        i1 = intArrayOf(0),
        i2 = intArrayOf(1),
        i3 = intArrayOf(2),
        colors = colors,
        types = types,
        alphas = alphas
    )

    private class FakeModelDefinition(
        private val xs: IntArray,
        private val ys: IntArray,
        private val zs: IntArray,
        private val i1: IntArray,
        private val i2: IntArray,
        private val i3: IntArray,
        private val colors: ShortArray,
        private val types: ByteArray? = null,
        private val alphas: ByteArray? = null
    ) : ModelDefinition {
        override fun getName() = "fake"
        override fun getVertexCount() = xs.size
        override fun getVertexPositionsX() = xs
        override fun getVertexPositionsY() = ys
        override fun getVertexPositionsZ() = zs
        override fun getVertexSkins(): IntArray? = null
        override fun getVertexGroups(): Array<IntArray>? = null
        override fun getVertexNormals(): Array<VertexNormal>? = null
        override fun getFaceCount() = i1.size
        override fun getFaceVertexIndices1() = i1
        override fun getFaceVertexIndices2() = i2
        override fun getFaceVertexIndices3() = i3
        override fun getFaceSkins(): IntArray? = null
        override fun getFaceGroups(): Array<IntArray>? = null
        override fun getFaceColors() = colors
        override fun getFaceAlphas() = alphas
        override fun getFacePriorities(): ByteArray? = null
        override fun getFaceTypes() = types
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
