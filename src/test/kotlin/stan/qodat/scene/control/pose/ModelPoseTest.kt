package stan.qodat.scene.control.pose

import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.model.ModelSkeleton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelPoseTest {

    @Test
    fun identityPoseLeavesVerticesUnchanged() {
        val skeleton = ModelSkeleton(limbModel())
        skeleton.ensureScratchBuffers()
        skeleton.applyPose(ModelPose(), parentChildSkeleton())
        assertEquals(listOf(0, 0, 0), skeleton.xyz(0))
        assertEquals(listOf(32, 0, 0), skeleton.xyz(2))
    }

    @Test
    fun rotatingParentMovesDescendantsAroundParentCentroid() {
        val skeleton = ModelSkeleton(limbModel())
        val pose = ModelPose()
        pose.addRotate(0, 0, 64, 0)

        skeleton.ensureScratchBuffers()
        skeleton.applyPose(pose, parentChildSkeleton())

        // Group 0 centroid is (8, 0, 0). Yaw 90°: (x, y, z) → (z, y, -(x - 8) + 0) relative.
        assertEquals(listOf(8, 0, 8), skeleton.xyz(0))
        assertEquals(listOf(8, 0, -8), skeleton.xyz(1))
        assertEquals(listOf(8, 0, -24), skeleton.xyz(2))
        assertEquals(listOf(8, 0, -40), skeleton.xyz(3))
        assertEquals(listOf(0, 0, 64), skeleton.xyz(4))
    }

    @Test
    fun rotatingParentDoesNotMoveUnrelatedGroups() {
        val skeleton = ModelSkeleton(limbModel())
        val pose = ModelPose()
        pose.addRotate(0, 0, 64, 0)

        skeleton.ensureScratchBuffers()
        skeleton.applyPose(pose, parentChildSkeleton())

        assertEquals(listOf(0, 0, 64), skeleton.xyz(4))
        assertEquals(listOf(16, 0, 64), skeleton.xyz(5))
    }

    @Test
    fun translateMovesTheBoneAndItsChildren() {
        val skeleton = ModelSkeleton(limbModel())
        val pose = ModelPose()
        pose.addTranslate(0, 5, -2, 3)

        skeleton.ensureScratchBuffers()
        skeleton.applyPose(pose, parentChildSkeleton())

        assertEquals(listOf(5, -2, 3), skeleton.xyz(0))
        assertEquals(listOf(37, -2, 3), skeleton.xyz(2))
        assertEquals(listOf(0, 0, 64), skeleton.xyz(4))
    }

    @Test
    fun poseStacksOnTopOfAnAnimationFrame() {
        val skeleton = ModelSkeleton(limbModel())
        skeleton.animate(
            AnimationFrameLegacy("frame", definition = null, duration = 1).apply {
                transformationList.add(
                    Transformation("t", intArrayOf(0, 1), TransformationType.TRANSLATE.ordinal, 10, 0, 0)
                )
            }
        )
        val pose = ModelPose()
        pose.addTranslate(1, 0, 4, 0)
        skeleton.applyPose(pose, parentChildSkeleton())

        assertEquals(listOf(10, 0, 0), skeleton.xyz(0))
        assertEquals(listOf(42, 4, 0), skeleton.xyz(2))
        assertEquals(listOf(0, 0, 64), skeleton.xyz(4))
    }

    @Test
    fun rotatingAChildGroupDoesNotThrowOnASmallSkin() {
        val skeleton = ModelSkeleton(limbModel())
        val pose = ModelPose()
        pose.addRotate(1, 0, 64, 0)
        skeleton.ensureScratchBuffers()
        skeleton.applyPose(pose, parentChildSkeleton())
        assertEquals(listOf(0, 0, 0), skeleton.xyz(0))
        assertTrue(skeleton.xyz(2) != listOf(32, 0, 0))
    }

    @Test
    fun rotationUnitsWrapAt256() {
        val pose = ModelPose()
        pose.addRotate(0, 200, 0, 0)
        pose.addRotate(0, 70, 0, 0)
        assertEquals(14, pose.delta(0).rx)
    }

    private fun parentChildSkeleton() = PoseSkeleton(
        listOf(
            PoseBone(0, null, intArrayOf(0, 1)),
            PoseBone(1, 0, intArrayOf(1)),
            PoseBone(2, null, intArrayOf(2)),
        )
    )

    /**
     * Group 0: verts 0,1 at (0,0,0) and (16,0,0) — centroid (8,0,0)
     * Group 1: verts 2,3 at (32,0,0) and (48,0,0) — a child limb
     * Group 2: verts 4,5 at (0,0,64) and (16,0,64) — unrelated
     */
    private fun limbModel() = QodatModelDefinition(
        name = "limb",
        vertexCount = 6,
        vertexPositionsX = intArrayOf(0, 16, 32, 48, 0, 16),
        vertexPositionsY = intArrayOf(0, 0, 0, 0, 0, 0),
        vertexPositionsZ = intArrayOf(0, 0, 0, 0, 64, 64),
        vertexSkins = intArrayOf(0, 0, 1, 1, 2, 2),
        faceCount = 2,
        faceVertexIndices1 = intArrayOf(0, 4),
        faceVertexIndices2 = intArrayOf(1, 5),
        faceVertexIndices3 = intArrayOf(2, 4),
        faceSkins = intArrayOf(0, 2),
        faceAlphas = null,
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0, 0),
    )

    private fun ModelSkeleton.xyz(vertex: Int) = listOf(getX(vertex), getY(vertex), getZ(vertex))
}
