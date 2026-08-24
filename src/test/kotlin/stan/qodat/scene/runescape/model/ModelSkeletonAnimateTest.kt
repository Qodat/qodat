package stan.qodat.scene.runescape.model

import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Synthetic [ModelSkeleton.animate] coverage for legacy frames:
 * vertex group transforms, type-5 / [TransformationType.TRANSPARENCY] alpha, empty no-ops.
 */
class ModelSkeletonAnimateTest {

    @Test
    fun emptyFrameLeavesVerticesAndAlphasUnchanged() {
        val definition = defaultModel()
        val skeleton = ModelSkeleton(definition)

        skeleton.animate(legacyFrame())

        assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
        assertAlphas(skeleton, 100, 50)
        assertFalse(skeleton.pullAlphaChanged())
    }

    @Test
    fun emptyFrameAfterTranslateRestoresOriginalVertices() {
        val definition = defaultModel()
        val skeleton = ModelSkeleton(definition)

        skeleton.animate(legacyFrame(transform(TransformationType.TRANSLATE, intArrayOf(0), 10, 20, 30)))
        assertEquals(listOf(10, 20, 30), skeleton.xyz(0))
        assertEquals(listOf(138, 20, 30), skeleton.xyz(1))

        skeleton.animate(legacyFrame())

        assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
    }

    @Test
    fun disabledAndUndefinedTransformsAreNoOps() {
        val definition = defaultModel()
        val skeleton = ModelSkeleton(definition)
        val disabled = transform(TransformationType.TRANSLATE, intArrayOf(0), 99, 99, 99).apply {
            enabledProperty.set(false)
        }

        skeleton.animate(
            legacyFrame(
                disabled,
                transform(TransformationType.UNDEFINED, intArrayOf(0), 7, 8, 9),
                Transformation("cache-type-5", intArrayOf(0), type = 5, deltaX = 8),
            )
        )

        assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
        assertAlphas(skeleton, 100, 50)
        assertFalse(skeleton.pullAlphaChanged())
    }

    @Test
    fun translateMovesOnlyTargetVertexGroup() {
        val skeleton = ModelSkeleton(defaultModel())

        skeleton.animate(legacyFrame(transform(TransformationType.TRANSLATE, intArrayOf(0), 5, -1, 2)))

        assertEquals(listOf(5, -1, 2), skeleton.xyz(0))
        assertEquals(listOf(133, -1, 2), skeleton.xyz(1))
        assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
        assertEquals(listOf(0, 0, 128), skeleton.xyz(3))
    }

    @Test
    fun scaleAroundOriginDoublesTargetVertices() {
        val skeleton = ModelSkeleton(defaultModel())

        skeleton.animate(legacyFrame(transform(TransformationType.SCALE, intArrayOf(0), 256, 256, 256)))

        assertEquals(listOf(0, 0, 0), skeleton.xyz(0))
        assertEquals(listOf(256, 0, 0), skeleton.xyz(1))
        assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
    }

    @Test
    fun rotateNinetyDegreesYawAroundOrigin() {
        val skeleton = ModelSkeleton(defaultModel())

        // convertRotationValue(64) = 512 → 90°; (x, y, z) → (z, y, -x)
        skeleton.animate(legacyFrame(transform(TransformationType.ROTATE, intArrayOf(0), 0, 64, 0)))

        assertEquals(listOf(0, 0, 0), skeleton.xyz(0))
        assertEquals(listOf(0, 0, -128), skeleton.xyz(1))
        assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
    }

    @Test
    fun setOffsetDoesNotMoveVerticesButPivotsScale() {
        val skeleton = ModelSkeleton(defaultModel())

        skeleton.animate(
            legacyFrame(
                transform(TransformationType.SET_OFFSET, intArrayOf(0)),
                transform(TransformationType.SCALE, intArrayOf(0), 256, 128, 128),
            )
        )

        // Group 0 centroid is (64, 0, 0); scale X×2 around that pivot.
        assertEquals(listOf(-64, 0, 0), skeleton.xyz(0))
        assertEquals(listOf(192, 0, 0), skeleton.xyz(1))
        assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
    }

    @Test
    fun setOffsetThenRotateNinetyDegreesYawAroundCentroid() {
        val skeleton = ModelSkeleton(defaultModel())

        skeleton.animate(
            legacyFrame(
                transform(TransformationType.SET_OFFSET, intArrayOf(0)),
                transform(TransformationType.ROTATE, intArrayOf(0), 0, 64, 0),
            )
        )

        assertEquals(listOf(64, 0, 64), skeleton.xyz(0))
        assertEquals(listOf(64, 0, -64), skeleton.xyz(1))
        assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
    }

    @Test
    fun outOfRangeGroupIndexIsSkipped() {
        val definition = defaultModel()
        val skeleton = ModelSkeleton(definition)

        skeleton.animate(legacyFrame(transform(TransformationType.TRANSLATE, intArrayOf(99), 50, 50, 50)))

        assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
    }

    @Test
    fun type5TransparencyAddsDeltaTimesEightToTargetFaceGroup() {
        val definition = defaultModel()
        val skeleton = ModelSkeleton(definition)

        // Client type-5: alpha += deltaX * 8, clamped to 0..255, applied to face groups.
        skeleton.animate(
            legacyFrame(transform(TransformationType.TRANSPARENCY, intArrayOf(0), deltaX = 4))
        )

        assertAlphas(skeleton, 132, 50)
        assertTrue(skeleton.pullAlphaChanged())
        assertEquals(100, definition.getFaceAlphas()!![0].toInt() and 255)
    }

    @Test
    fun type5TransparencyClampsAndRestoresOnLaterEmptyFrame() {
        val skeleton = ModelSkeleton(
            defaultModel(faceAlphas = byteArrayOf(250.toByte(), 10))
        )

        skeleton.animate(
            legacyFrame(transform(TransformationType.TRANSPARENCY, intArrayOf(0, 1), deltaX = 2))
        )
        assertAlphas(skeleton, 255, 26)
        assertTrue(skeleton.pullAlphaChanged())

        skeleton.animate(
            legacyFrame(transform(TransformationType.TRANSPARENCY, intArrayOf(1), deltaX = -4))
        )
        assertAlphas(skeleton, 250, 0)
        assertTrue(skeleton.pullAlphaChanged())

        skeleton.animate(legacyFrame())
        assertAlphas(skeleton, 250, 10)
        assertTrue(skeleton.pullAlphaChanged())
        assertFalse(skeleton.pullAlphaChanged())
    }

    @Test
    fun type5TransparencySeedsZeroWhenDefinitionHasNoAlphas() {
        val skeleton = ModelSkeleton(defaultModel(faceAlphas = null))

        skeleton.animate(
            legacyFrame(transform(TransformationType.TRANSPARENCY, intArrayOf(0), deltaX = 3))
        )

        assertAlphas(skeleton, 24, 0)
        assertTrue(skeleton.pullAlphaChanged())
    }

    private fun defaultModel(
        faceAlphas: ByteArray? = byteArrayOf(100, 50),
    ) = QodatModelDefinition(
        name = "synthetic",
        vertexCount = 4,
        vertexPositionsX = intArrayOf(0, 128, 0, 0),
        vertexPositionsY = intArrayOf(0, 0, 128, 0),
        vertexPositionsZ = intArrayOf(0, 0, 0, 128),
        vertexSkins = intArrayOf(0, 0, 1, 1),
        faceCount = 2,
        faceVertexIndices1 = intArrayOf(0, 0),
        faceVertexIndices2 = intArrayOf(1, 1),
        faceVertexIndices3 = intArrayOf(2, 3),
        faceSkins = intArrayOf(0, 1),
        faceAlphas = faceAlphas?.copyOf(),
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0, 0),
    )

    private fun legacyFrame(vararg transforms: Transformation) =
        AnimationFrameLegacy("synthetic-frame", definition = null, duration = 1).apply {
            transformationList.addAll(transforms)
        }

    private fun transform(
        type: TransformationType,
        groups: IntArray,
        deltaX: Int = 0,
        deltaY: Int = 0,
        deltaZ: Int = 0,
    ) = Transformation("t-${type.name}", groups, type.ordinal, deltaX, deltaY, deltaZ)

    private fun ModelSkeleton.xyz(vertex: Int) = listOf(getX(vertex), getY(vertex), getZ(vertex))

    private fun ModelSkeleton.xyzSnapshot() = List(getVertexCount()) { xyz(it) }

    private fun QodatModelDefinition.xyzSnapshot() = List(getVertexCount()) {
        listOf(getVertexPositionsX()[it], getVertexPositionsY()[it], getVertexPositionsZ()[it])
    }

    private fun assertAlphas(skeleton: ModelSkeleton, vararg expected: Int) {
        val alphas = skeleton.getRenderFaceAlphas()
        assertTrue(alphas != null && alphas.size == expected.size)
        expected.forEachIndexed { i, value ->
            assertEquals(value, alphas!![i].toInt() and 255, "face $i alpha")
        }
    }
}
