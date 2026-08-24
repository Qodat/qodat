package stan.qodat.scene.control.export.wavefront

import qodat.cache.definition.ModelDefinition
import qodat.cache.models.FaceNormal
import qodat.cache.models.VertexNormal
import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WaveFrontUtilTest {

    @Test
    fun missingTexturesUseConvertedFaceColorAndDefaultAlpha() {
        val definition = ColorOnlyModelDefinition(colors = shortArrayOf(0))
        val material = definition.getFaceMaterial(0)
        val color = assertIs<WaveFrontMaterial.Color>(material)
        val expected = rs2Hsb(0)
        assertEquals(expected.red / 255.0, color.r, 1e-9)
        assertEquals(expected.green / 255.0, color.g, 1e-9)
        assertEquals(expected.blue / 255.0, color.b, 1e-9)
        assertEquals(0.0, color.alpha)
    }

    @Test
    fun textureIdMinusOneStillUsesColorPath() {
        val definition = ColorOnlyModelDefinition(
            colors = shortArrayOf(packedHsb(21, 7, 127)),
            textures = shortArrayOf((-1).toShort())
        )
        val material = assertIs<WaveFrontMaterial.Color>(definition.getFaceMaterial(0))
        val expected = rs2Hsb(packedHsb(21, 7, 127).toInt())
        assertEquals(expected.red / 255.0, material.r, 1e-9)
        assertEquals(expected.green / 255.0, material.g, 1e-9)
        assertEquals(expected.blue / 255.0, material.b, 1e-9)
    }

    @Test
    fun recolorMapReplacesTheLookedUpHsbBeforeConversion() {
        val source = 10.toShort()
        val replacement = packedHsb(0, 0, 127)
        val definition = ColorOnlyModelDefinition(colors = shortArrayOf(source))
        val withoutMap = assertIs<WaveFrontMaterial.Color>(definition.getFaceMaterial(0))
        val withMap = assertIs<WaveFrontMaterial.Color>(
            definition.getFaceMaterial(0, mapOf(source to replacement))
        )
        val expected = rs2Hsb(replacement.toInt())
        assertEquals(expected.red / 255.0, withMap.r, 1e-9)
        assertTrue(withoutMap.r != withMap.r || withoutMap.g != withMap.g || withoutMap.b != withMap.b)
    }

    @Test
    fun faceAlphaIsUnsignedAndScaledToUnitInterval() {
        val definition = ColorOnlyModelDefinition(
            colors = shortArrayOf(0, 0, 0),
            alphas = byteArrayOf(0, 0x80.toByte(), (-1).toByte())
        )
        assertEquals(0.0, (definition.getFaceMaterial(0) as WaveFrontMaterial.Color).alpha)
        assertEquals(128.0 / 255.0, (definition.getFaceMaterial(1) as WaveFrontMaterial.Color).alpha, 1e-9)
        assertEquals(1.0, (definition.getFaceMaterial(2) as WaveFrontMaterial.Color).alpha)
    }

    @Test
    fun getFaceMaterialsCoversEveryFaceAndDeduplicatesEqualColors() {
        val definition = ColorOnlyModelDefinition(
            colors = shortArrayOf(0, 0, packedHsb(10, 3, 40)),
            alphas = byteArrayOf(0, 0, 0)
        )
        val materials = definition.getFaceMaterials()
        assertEquals(3, materials.size)
        assertEquals(materials[0], materials[1])
        assertEquals(2, materials.toSet().size)
        assertEquals(0, ColorOnlyModelDefinition(colors = shortArrayOf()).getFaceMaterials().size)
    }

    @Test
    fun unusedRecolorEntriesAreIgnored() {
        val definition = ColorOnlyModelDefinition(colors = shortArrayOf(5))
        val original = definition.getFaceMaterial(0) as WaveFrontMaterial.Color
        val remapped = definition.getFaceMaterial(0, mapOf(99.toShort() to packedHsb(0, 7, 127))) as WaveFrontMaterial.Color
        assertEquals(original, remapped)
    }

    private fun packedHsb(hue: Int, saturation: Int, brightness: Int): Short =
        ((hue and 0x3f shl 10) or (saturation and 0x07 shl 7) or (brightness and 0x7f)).toShort()

    private fun rs2Hsb(hsb: Int): Color {
        val hue = hsb shr 10 and 0x3f
        val saturation = hsb shr 7 and 0x07
        val brightness = hsb and 0x7f
        return Color.getHSBColor(hue.toFloat() / 63, saturation.toFloat() / 7, brightness.toFloat() / 127)
    }

    private class ColorOnlyModelDefinition(
        private val colors: ShortArray,
        private val alphas: ByteArray? = null,
        private val textures: ShortArray? = null,
    ) : ModelDefinition {
        override fun getName() = "color-only"
        override fun getVertexCount() = 0
        override fun getVertexPositionsX() = intArrayOf()
        override fun getVertexPositionsY() = intArrayOf()
        override fun getVertexPositionsZ() = intArrayOf()
        override fun getVertexSkins(): IntArray? = null
        override fun getVertexGroups(): Array<IntArray>? = null
        override fun getVertexNormals(): Array<VertexNormal>? = null
        override fun getFaceCount() = colors.size
        override fun getFaceVertexIndices1() = IntArray(colors.size)
        override fun getFaceVertexIndices2() = IntArray(colors.size)
        override fun getFaceVertexIndices3() = IntArray(colors.size)
        override fun getFaceSkins(): IntArray? = null
        override fun getFaceGroups(): Array<IntArray>? = null
        override fun getFaceColors() = colors
        override fun getFaceAlphas() = alphas
        override fun getFacePriorities(): ByteArray? = null
        override fun getFaceTypes(): ByteArray? = null
        override fun getFaceNormals(): Array<FaceNormal?>? = null
        override fun getPriority() = 0.toByte()
        override fun getTextureConfigCount() = 0
        override fun getTextureRenderTypes(): ByteArray? = null
        override fun getFaceTextures() = textures
        override fun getFaceTextureConfigs(): ByteArray? = null
        override fun getTextureTriangleVertexIndices1(): ShortArray? = null
        override fun getTextureTriangleVertexIndices2(): ShortArray? = null
        override fun getTextureTriangleVertexIndices3(): ShortArray? = null
        override fun getFaceTextureUCoordinates(): Array<FloatArray>? = null
        override fun getFaceTextureVCoordinates(): Array<FloatArray>? = null
        override fun computeAnimationTables() = Unit
        override fun computeTextureUVCoordinates() = Unit
        override fun computeNormals() = Unit
    }
}
