package stan.qodat.scene.runescape.model

import jagex.MayaAnimation
import jagex.MayaAnimationSupport
import jagex.MayaAnimationSupport.FLAG_CHANNEL_0
import jagex.MayaAnimationSupport.FLAG_CHANNEL_TRANSLATE_X
import jagex.MayaAnimationSupport.TYPE_SECONDARY
import jagex.MayaAnimationSupport.TYPE_TRANSFORMATION
import jagex.Skeleton
import qodat.cache.models.RS2Model
import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrameMaya
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Synthetic [ModelSkeleton.animate] coverage for Maya frames:
 * identity bind pose, secondary-channel translation, type-5 alpha, missing skeleton.
 * Uses [RS2Model] only — not the JavaFX [Model].
 */
class ModelSkeletonMayaAnimateTest {

    @Test
    fun identityBindPoseLeavesVerticesUnchanged() {
        MayaAnimationSupport.withStubIndex { index ->
            val definition = syntheticRs2Model()
            val skeleton = ModelSkeleton(definition)
            val anim = MayaAnimationSupport.load(index, MayaAnimationSupport.identitySkeleton())

            skeleton.animate(mayaFrame(anim))

            assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
            assertAlphas(skeleton, 100, 50)
            assertFalse(skeleton.pullAlphaChanged())
        }
    }

    @Test
    fun secondaryTranslateXMovesOnlyMayaBoundVertex() {
        MayaAnimationSupport.withStubIndex { index ->
            val definition = syntheticRs2Model()
            val skeleton = ModelSkeleton(definition)
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
                MayaAnimationSupport.animationBytes(
                    curves = listOf(
                        MayaAnimationSupport.constantCurve(
                            value = 10f,
                            type = TYPE_SECONDARY,
                            trackIndex = 0,
                            flag = FLAG_CHANNEL_TRANSLATE_X,
                        ),
                    ),
                ),
            )

            skeleton.animate(mayaFrame(anim))

            assertEquals(listOf(10, 0, 0), skeleton.xyz(0))
            assertEquals(listOf(128, 0, 0), skeleton.xyz(1))
            assertEquals(listOf(0, 128, 0), skeleton.xyz(2))
        }
    }

    @Test
    fun outOfRangeTranslateHoldsFirstKeyframeInsteadOfOrigin() {
        MayaAnimationSupport.withStubIndex { index ->
            val definition = syntheticRs2Model()
            val skeleton = ModelSkeleton(definition)
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
                MayaAnimationSupport.animationBytes(
                    curves = listOf(
                        MayaAnimationSupport.constantCurve(
                            value = 10f,
                            frameNumber = 3,
                            type = TYPE_SECONDARY,
                            trackIndex = 0,
                            flag = FLAG_CHANNEL_TRANSLATE_X,
                        ),
                    ),
                ),
            )

            skeleton.animate(mayaFrame(anim, index = 0))

            assertEquals(listOf(10, 0, 0), skeleton.xyz(0))
        }
    }

    @Test
    fun type5TransparencyAddsEvaluatedAmountToTargetFaceGroup() {
        MayaAnimationSupport.withStubIndex { index ->
            val definition = syntheticRs2Model()
            val skeleton = ModelSkeleton(definition)
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
                MayaAnimationSupport.animationBytes(
                    curves = listOf(
                        MayaAnimationSupport.constantCurve(
                            value = 0.04f,
                            type = TYPE_TRANSFORMATION,
                            trackIndex = 0,
                            flag = FLAG_CHANNEL_0,
                        ),
                    ),
                ),
            )

            skeleton.animate(mayaFrame(anim))

            assertAlphas(skeleton, 110, 50)
            assertTrue(skeleton.pullAlphaChanged())
            assertEquals(100, definition.getFaceAlphas()!![0].toInt() and 255)
        }
    }

    @Test
    fun missingMayaSkeletonIsANoOp() {
        MayaAnimationSupport.withStubIndex { index ->
            val definition = syntheticRs2Model()
            val skeleton = ModelSkeleton(definition)
            val anim = MayaAnimationSupport.load(index, MayaAnimationSupport.identitySkeleton())
            anim.skeleton = Skeleton(1, MayaAnimationSupport.skeletonWithoutMayaBones())

            skeleton.animate(mayaFrame(anim))

            assertEquals(definition.xyzSnapshot(), skeleton.xyzSnapshot())
            assertAlphas(skeleton, 100, 50)
            assertFalse(skeleton.pullAlphaChanged())
        }
    }

    @Test
    fun nonRs2ModelIsRejected() {
        MayaAnimationSupport.withStubIndex { index ->
            val anim = MayaAnimationSupport.load(index, MayaAnimationSupport.identitySkeleton())
            val skeleton = ModelSkeleton(qodatModel())

            val error = assertFailsWith<IllegalStateException> {
                skeleton.animate(mayaFrame(anim))
            }
            assertEquals("ModelDefinition is not an RS2Model", error.message)
        }
    }

    private fun mayaFrame(animation: MayaAnimation, index: Int = 0) =
        AnimationFrameMaya("maya-frame", duration = 1, index = index, animation = animation)

    private fun syntheticRs2Model(
        faceAlphas: ByteArray = byteArrayOf(100, 50),
    ) = RS2Model().apply {
        setVertexCount(4)
        setVertexPositionsX(intArrayOf(0, 128, 0, 0))
        setVertexPositionsY(intArrayOf(0, 0, 128, 0))
        setVertexPositionsZ(intArrayOf(0, 0, 0, 128))
        setVertexSkins(intArrayOf(0, 0, 1, 1))
        setFaceCount(2)
        setFaceVertexIndices1(intArrayOf(0, 0))
        setFaceVertexIndices2(intArrayOf(1, 1))
        setFaceVertexIndices3(intArrayOf(2, 3))
        setFaceSkins(intArrayOf(0, 1))
        setFaceAlphas(faceAlphas.copyOf())
        setFaceColors(shortArrayOf(0, 0))
        setMayaGroups(arrayOf(intArrayOf(0), intArrayOf(), intArrayOf(), intArrayOf()))
        setMayaScales(arrayOf(intArrayOf(255), intArrayOf(), intArrayOf(), intArrayOf()))
    }

    private fun qodatModel() = QodatModelDefinition(
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
        faceAlphas = byteArrayOf(100, 50),
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0, 0),
    )

    private fun ModelSkeleton.xyz(vertex: Int) = listOf(getX(vertex), getY(vertex), getZ(vertex))

    private fun ModelSkeleton.xyzSnapshot() = List(getVertexCount()) { xyz(it) }

    private fun RS2Model.xyzSnapshot() = List(getVertexCount()) {
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
