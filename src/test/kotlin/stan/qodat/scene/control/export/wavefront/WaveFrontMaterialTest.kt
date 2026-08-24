package stan.qodat.scene.control.export.wavefront

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveFrontMaterialTest {

    @Test
    fun colorMtlWritesDiffuseAndOmitsZeroAlpha() {
        val mtl = captureMtl(WaveFrontMaterial.Color(0.1, 0.2, 0.3, 0.0))
        assertEquals(listOf("Kd 0.1 0.2 0.3"), mtl)
    }

    @Test
    fun colorMtlWritesAlphaWhenPositive() {
        val mtl = captureMtl(WaveFrontMaterial.Color(1.0, 0.0, 0.5, 0.25))
        assertEquals(listOf("Kd 1.0 0.0 0.5", "d 0.25"), mtl)
    }

    @Test
    fun tryEncodeAlphaIgnoresNonPositiveValues() {
        val zero = StringWriter()
        WaveFrontMaterial.Color(1.0, 1.0, 1.0, 0.0).tryEncodeAlpha(PrintWriter(zero, true))
        assertEquals("", zero.toString())

        val negative = StringWriter()
        WaveFrontMaterial.Color(1.0, 1.0, 1.0, -0.5).tryEncodeAlpha(PrintWriter(negative, true))
        assertEquals("", negative.toString())

        val positive = StringWriter()
        WaveFrontMaterial.Color(1.0, 1.0, 1.0, 1.0).tryEncodeAlpha(PrintWriter(positive, true))
        assertEquals("d 1.0${System.lineSeparator()}", positive.toString())
    }

    @Test
    fun colorObjWritesOneBasedTriangleWithoutUv() {
        val obj = captureObj(WaveFrontMaterial.Color(0.0, 0.0, 0.0, 0.0), face = 4, x = 1, y = 2, z = 3)
        assertEquals(listOf("f 1 2 3"), obj)
    }

    @Test
    fun textureObjWritesUvIndicesFromFaceOffset() {
        val first = captureObj(WaveFrontMaterial.Texture(12, 0.0), face = 0, x = 1, y = 2, z = 3)
        assertEquals(listOf("f 1/1 2/2 3/3"), first)

        val second = captureObj(WaveFrontMaterial.Texture(12, 0.0), face = 1, x = 10, y = 20, z = 30)
        assertEquals(listOf("f 10/4 20/5 30/6"), second)
    }

    @Test
    fun colorEqualityCollapsesDuplicateFacesInASet() {
        val materials = setOf(
            WaveFrontMaterial.Color(0.2, 0.4, 0.6, 0.0),
            WaveFrontMaterial.Color(0.2, 0.4, 0.6, 0.0),
            WaveFrontMaterial.Color(0.9, 0.1, 0.1, 1.0)
        )
        assertEquals(2, materials.size)
        assertTrue(WaveFrontMaterial.Color(0.2, 0.4, 0.6, 0.0) in materials)
        assertFalse(WaveFrontMaterial.Color(0.0, 0.0, 0.0, 0.0) in materials)
    }

    private fun captureMtl(material: WaveFrontMaterial): List<String> {
        val writer = StringWriter()
        material.encodeMtl(PrintWriter(writer, true), Path.of("unused"))
        return writer.toString().lines().filter { it.isNotEmpty() }
    }

    private fun captureObj(material: WaveFrontMaterial, face: Int, x: Int, y: Int, z: Int): List<String> {
        val writer = StringWriter()
        material.encodeObj(PrintWriter(writer, true), face, x, y, z)
        return writer.toString().lines().filter { it.isNotEmpty() }
    }
}
