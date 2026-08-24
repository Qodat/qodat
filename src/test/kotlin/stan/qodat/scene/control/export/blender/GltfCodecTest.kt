package stan.qodat.scene.control.export.blender

import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class GltfCodecTest {

    @Test
    fun writesTileMetreScaleAndRoundTripsIntegerVertices() {
        val dir = Files.createTempDirectory("gltf-scale")
        try {
            val file = dir.resolve("human.glb")
            GltfCodec.write(humanDefinition(), file, "human")

            val doc = GltfCodec.readDocument(file)
            val rs = doc.getAsJsonObject("extras").getAsJsonObject("rs")
            assertEquals(128f, rs.get("rsUnitsPerTile").asFloat)
            assertEquals(1f, rs.get("metersPerTile").asFloat)
            assertEquals(GltfCodec.UNIT_SCALE, rs.get("unitScale").asFloat)

            val read = GltfCodec.read(file)
            assertEquals(listOf(0, 128, 0, 0), read.getVertexPositionsX().toList())
            assertEquals(listOf(0, 0, 180, 0), read.getVertexPositionsY().toList())
            assertEquals(listOf(0, 0, 0, 128), read.getVertexPositionsZ().toList())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun bindPosePositionsAreOneTileEqualsOneMetre() {
        val dir = Files.createTempDirectory("gltf-tile")
        try {
            val file = dir.resolve("tile.glb")
            GltfCodec.write(humanDefinition(), file, "tile")
            val doc = GltfCodec.readDocument(file)
            val acc = doc.getAsJsonArray("accessors")[0].asJsonObject
            val max = acc.getAsJsonArray("max")
            assertEquals(1.0, max[0].asFloat.toDouble(), 1e-5)
            assertEquals(0.0, max[1].asFloat.toDouble(), 1e-5)
            assertEquals(0.0, max[2].asFloat.toDouble(), 1e-5)
            val min = acc.getAsJsonArray("min")
            assertEquals(0.0, min[0].asFloat.toDouble(), 1e-5)
            assertEquals(-180.0 / 128.0, min[1].asFloat.toDouble(), 1e-5)
            assertEquals(-1.0, min[2].asFloat.toDouble(), 1e-5)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun bakesIdleAndWalkAsArmatureActions() {
        val dir = Files.createTempDirectory("gltf-clips")
        try {
            val file = dir.resolve("anim.glb")
            val idle = clipFrame(TransformationType.TRANSLATE, 0, 0, 0)
            val walk = clipFrame(TransformationType.TRANSLATE, 128, 0, 0)
            GltfCodec.write(
                humanDefinition(),
                file,
                "anim",
                listOf(
                    GltfAnimationClip("Idle", listOf(idle)),
                    GltfAnimationClip("Walk", listOf(walk)),
                ),
            )

            val doc = GltfCodec.readDocument(file)
            val rs = doc.getAsJsonObject("extras").getAsJsonObject("rs")
            assertEquals(50, rs.get("frameRate").asInt)
            assertEquals(20, rs.get("clientTickMs").asInt)
            assertEquals("armature", rs.get("animation").asString)

            val animations = doc.getAsJsonArray("animations")
            assertEquals(listOf("Idle", "Walk"), animations.map { it.asJsonObject.get("name").asString })

            val prim = doc.getAsJsonArray("meshes")[0].asJsonObject.getAsJsonArray("primitives")[0].asJsonObject
            assertEquals(false, prim.has("targets"))

            val idleAnim = animations[0].asJsonObject
            val channels = idleAnim.getAsJsonArray("channels")
            assertEquals(4, channels.size())
            assertEquals("translation", channels[0].asJsonObject.getAsJsonObject("target").get("path").asString)
            assertEquals("rotation", channels[1].asJsonObject.getAsJsonObject("target").get("path").asString)
            assertEquals(1, channels[0].asJsonObject.getAsJsonObject("target").get("node").asInt)
            assertEquals("STEP", idleAnim.getAsJsonArray("samplers")[0].asJsonObject.get("interpolation").asString)
            assertEquals(50, idleAnim.getAsJsonObject("extras").get("frameRate").asInt)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private fun humanDefinition() = QodatModelDefinition(
        name = "human",
        vertexCount = 4,
        vertexPositionsX = intArrayOf(0, 128, 0, 0),
        vertexPositionsY = intArrayOf(0, 0, 180, 0),
        vertexPositionsZ = intArrayOf(0, 0, 0, 128),
        vertexSkins = intArrayOf(0, 0, 1, 1),
        faceCount = 2,
        faceVertexIndices1 = intArrayOf(0, 0),
        faceVertexIndices2 = intArrayOf(1, 1),
        faceVertexIndices3 = intArrayOf(2, 3),
        faceSkins = intArrayOf(0, 1),
        faceAlphas = null,
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0, 0),
    )

    private fun clipFrame(type: TransformationType, dx: Int, dy: Int, dz: Int) =
        AnimationFrameLegacy("frame", definition = null, duration = 5).apply {
            transformationList.add(Transformation("t", intArrayOf(0), type.ordinal, dx, dy, dz))
        }
}
