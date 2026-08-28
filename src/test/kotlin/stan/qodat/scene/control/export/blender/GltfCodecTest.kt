package stan.qodat.scene.control.export.blender

import qodat.cache.definition.TextureDefinition
import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun appliesNpcResizeOnTopOfTileMetreScale() {
        val dir = Files.createTempDirectory("gltf-resize")
        try {
            val file = dir.resolve("angela.glb")
            val scale = GltfEntityScale(x = 48, y = 48, z = 48)
            GltfCodec.write(humanDefinition(), file, "angela", entityScale = scale)

            val doc = GltfCodec.readDocument(file)
            val rs = doc.getAsJsonObject("extras").getAsJsonObject("rs")
            assertEquals(48, rs.get("resizeX").asInt)
            assertEquals(48, rs.get("resizeY").asInt)
            assertEquals(48, rs.get("resizeZ").asInt)

            val acc = doc.getAsJsonArray("accessors")[0].asJsonObject
            val max = acc.getAsJsonArray("max")
            assertEquals(48.0 / 128.0, max[0].asFloat.toDouble(), 1e-5)
            assertEquals(0.0, max[1].asFloat.toDouble(), 1e-5)
            assertEquals(0.0, max[2].asFloat.toDouble(), 1e-5)
            val min = acc.getAsJsonArray("min")
            assertEquals(0.0, min[0].asFloat.toDouble(), 1e-5)
            assertEquals(-180.0 * 48.0 / 128.0 / 128.0, min[1].asFloat.toDouble(), 1e-5)
            assertEquals(-48.0 / 128.0, min[2].asFloat.toDouble(), 1e-5)

            val read = GltfCodec.read(file)
            assertEquals(listOf(0, 128, 0, 0), read.getVertexPositionsX().toList())
            assertEquals(listOf(0, 0, 180, 0), read.getVertexPositionsY().toList())
            assertEquals(listOf(0, 0, 0, 128), read.getVertexPositionsZ().toList())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun identityResizeLeavesTileMetrePositionsUnchanged() {
        val dir = Files.createTempDirectory("gltf-identity-resize")
        try {
            val file = dir.resolve("human.glb")
            GltfCodec.write(humanDefinition(), file, "human", entityScale = GltfEntityScale.IDENTITY)
            val doc = GltfCodec.readDocument(file)
            val rs = doc.getAsJsonObject("extras").getAsJsonObject("rs")
            assertEquals(128, rs.get("resizeX").asInt)
            val max = doc.getAsJsonArray("accessors")[0].asJsonObject.getAsJsonArray("max")
            assertEquals(1.0, max[0].asFloat.toDouble(), 1e-5)
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

    @Test
    fun embedsFaceTextureAsPngPrimitive() {
        val dir = Files.createTempDirectory("gltf-tex")
        try {
            val file = dir.resolve("lava.glb")
            val source = fireTextureSource()
            GltfCodec.write(texturedDefinition(), file, "lava", textures = source)

            val (doc, blob) = GltfCodec.readChunks(file)
            val primitives = doc.getAsJsonArray("meshes")[0].asJsonObject.getAsJsonArray("primitives")
            assertEquals(2, primitives.size())
            assertEquals(false, primitives[0].asJsonObject.getAsJsonObject("attributes").has("TEXCOORD_0"))
            assertTrue(primitives[1].asJsonObject.getAsJsonObject("attributes").has("TEXCOORD_0"))

            val images = doc.getAsJsonArray("images")
            assertEquals(1, images.size())
            assertEquals("image/png", images[0].asJsonObject.get("mimeType").asString)

            val sampler = doc.getAsJsonArray("samplers")[0].asJsonObject
            assertEquals(10497, sampler.get("wrapS").asInt)
            assertEquals(10497, sampler.get("wrapT").asInt)

            val material = doc.getAsJsonArray("materials")[1].asJsonObject
            assertEquals("BLEND", material.get("alphaMode").asString)
            assertEquals(40, material.getAsJsonObject("extras").get("rsTextureId").asInt)

            val view = doc.getAsJsonArray("bufferViews")
                .get(images[0].asJsonObject.get("bufferView").asInt).asJsonObject
            val off = view.get("byteOffset").asInt
            val len = view.get("byteLength").asInt
            val png = ImageIO.read(ByteArrayInputStream(blob.copyOfRange(off, off + len)))
            assertEquals(2, png.width)
            assertEquals(2, png.height)
            assertEquals(0xFFFF0000.toInt(), png.getRGB(0, 0))
            assertEquals(0, png.getRGB(1, 0) ushr 24)

            val extras = doc.getAsJsonObject("extras").getAsJsonObject("rs")
            assertEquals(listOf(-1, 40), extras.getAsJsonArray("faceTextures").map { it.asInt.toShort().toInt() })
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun fullRoundTripPreservesVerticesFacesSkinsAndTextures() {
        val dir = Files.createTempDirectory("gltf-roundtrip")
        try {
            val file = dir.resolve("roundtrip.glb")
            val original = texturedDefinition()
            original.computeTextureUVCoordinates()
            GltfCodec.write(original, file, original.getName(), textures = fireTextureSource())

            val read = GltfCodec.read(file)
            assertEquals(original.getName(), read.getName())
            assertEquals(original.getVertexCount(), read.getVertexCount())
            assertEquals(original.getFaceCount(), read.getFaceCount())
            assertContentEquals(original.getVertexPositionsX(), read.getVertexPositionsX())
            assertContentEquals(original.getVertexPositionsY(), read.getVertexPositionsY())
            assertContentEquals(original.getVertexPositionsZ(), read.getVertexPositionsZ())
            assertContentEquals(original.getVertexSkins(), read.getVertexSkins())
            assertContentEquals(original.getFaceVertexIndices1(), read.getFaceVertexIndices1())
            assertContentEquals(original.getFaceVertexIndices2(), read.getFaceVertexIndices2())
            assertContentEquals(original.getFaceVertexIndices3(), read.getFaceVertexIndices3())
            assertContentEquals(original.getFaceSkins(), read.getFaceSkins())
            assertContentEquals(original.getFaceColors(), read.getFaceColors())
            assertContentEquals(original.getFaceTextures(), read.getFaceTextures())
            assertContentEquals(original.getFaceTextureConfigs(), read.getFaceTextureConfigs())
            read.computeTextureUVCoordinates()
            val srcU = original.getFaceTextureUCoordinates()!!
            val dstU = read.getFaceTextureUCoordinates()!!
            val srcV = original.getFaceTextureVCoordinates()!!
            val dstV = read.getFaceTextureVCoordinates()!!
            assertContentEquals(srcU[1], dstU[1])
            assertContentEquals(srcV[1], dstV[1])
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

    private fun texturedDefinition() = QodatModelDefinition(
        name = "lava",
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
        faceColors = shortArrayOf(10, 20),
        faceTextures = shortArrayOf((-1).toShort(), 40),
        faceTextureConfigs = byteArrayOf((-1).toByte(), (-1).toByte()),
    )

    private fun fireTextureSource(): GltfTextureSource {
        val image = GltfTextureSource.encode(object : TextureDefinition {
            override var id = 40
            override val fileIds = intArrayOf(99)
            override var pixels = intArrayOf(0x00FF0000, 0, 0x0000FF00, 0x000000FF)
            override val animationDirection get() = 1
            override val animationSpeed get() = 2
        })!!
        return GltfTextureSource { id -> if (id == 40) image else null }
    }

    private fun clipFrame(type: TransformationType, dx: Int, dy: Int, dz: Int) =
        AnimationFrameLegacy("frame", definition = null, duration = 5).apply {
            transformationList.add(Transformation("t", intArrayOf(0), type.ordinal, dx, dy, dz))
        }
}
