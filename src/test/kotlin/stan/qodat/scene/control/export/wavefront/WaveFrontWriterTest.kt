package stan.qodat.scene.control.export.wavefront

import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.model.ModelSkeleton
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveFrontWriterTest {

    @Test
    fun mtlFileNameReplacesSpacesAndEncodesMaterialsInOrder() {
        val dir = Files.createTempDirectory("wavefront-mtl")
        try {
            val materials = linkedSetOf(
                WaveFrontMaterial.Color(1.0, 0.0, 0.0, 0.0),
                WaveFrontMaterial.Color(0.0, 1.0, 0.0, 0.5)
            )
            WaveFrontWriter(dir).writeMtlFile(materials, "my model")

            val mtl = dir.resolve("my_model.mtl")
            assertTrue(Files.exists(mtl))
            assertFalse(Files.exists(dir.resolve("my model.mtl")))
            assertEquals(
                listOf(
                    "newmtl m0",
                    "Kd 1.0 0.0 0.0",
                    "newmtl m1",
                    "Kd 0.0 1.0 0.0",
                    "d 0.5"
                ),
                mtl.readText().lines().filter { it.isNotEmpty() }
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun mtlFileNameLeavesNonSpaceCharactersAndWritesEmptySet() {
        val dir = Files.createTempDirectory("wavefront-empty-mtl")
        try {
            WaveFrontWriter(dir).writeMtlFile(emptySet(), "a-b.v2")
            val text = dir.resolve("a-b.v2.mtl").readText()
            assertEquals("", text)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun objFileKeepsSpacesInNameNegatesYZAndUsesOneBasedFaces() {
        val dir = Files.createTempDirectory("wavefront-obj")
        try {
            val model = ModelSkeleton(triangleDefinition())
            val materials = model.modelDefinition.getFaceMaterials().toSet()
            WaveFrontWriter(dir).writeObjFile(
                model = model,
                materials = materials,
                mtlFileNameWithoutExtension = "my lib",
                objFileNameWithoutExtension = "my mesh"
            )

            val obj = dir.resolve("my mesh.obj")
            assertTrue(Files.exists(obj))
            val lines = obj.readText().lines()
            assertEquals("mtllib my lib.mtl", lines[0])
            assertEquals("o my mesh", lines[1])
            assertTrue(lines.contains("v 1 -4 -7"))
            assertTrue(lines.contains("v 2 -5 -8"))
            assertTrue(lines.contains("v 3 -6 -9"))
            assertFalse(lines.any { it.startsWith("vt ") })
            assertTrue(lines.any { it.startsWith("vn ") })
            assertTrue(lines.contains("usemtl m0"))
            assertTrue(lines.contains("f 1 2 3"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun objFileWithNoFacesWritesHeaderOnly() {
        val dir = Files.createTempDirectory("wavefront-empty-obj")
        try {
            val model = ModelSkeleton(emptyDefinition())
            WaveFrontWriter(dir).writeObjFile(
                model = model,
                materials = emptySet(),
                mtlFileNameWithoutExtension = "none",
                objFileNameWithoutExtension = "empty"
            )
            assertEquals(
                listOf("mtllib none.mtl", "o empty"),
                dir.resolve("empty.obj").readText().lines().filter { it.isNotEmpty() }
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private fun triangleDefinition() = QodatModelDefinition(
        name = "triangle",
        vertexCount = 3,
        vertexPositionsX = intArrayOf(1, 2, 3),
        vertexPositionsY = intArrayOf(4, 5, 6),
        vertexPositionsZ = intArrayOf(7, 8, 9),
        vertexSkins = null,
        faceCount = 1,
        faceVertexIndices1 = intArrayOf(0),
        faceVertexIndices2 = intArrayOf(1),
        faceVertexIndices3 = intArrayOf(2),
        faceSkins = null,
        faceAlphas = null,
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0)
    )

    private fun emptyDefinition() = QodatModelDefinition(
        name = "empty",
        vertexCount = 0,
        vertexPositionsX = intArrayOf(),
        vertexPositionsY = intArrayOf(),
        vertexPositionsZ = intArrayOf(),
        vertexSkins = null,
        faceCount = 0,
        faceVertexIndices1 = intArrayOf(),
        faceVertexIndices2 = intArrayOf(),
        faceVertexIndices3 = intArrayOf(),
        faceSkins = null,
        faceAlphas = null,
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf()
    )
}
