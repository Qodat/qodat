package stan.qodat.scene.control.pose

import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.control.tree.BucketHierarchyNode
import stan.qodat.scene.control.tree.TransformHierarchyNode
import stan.qodat.scene.control.tree.VertexGroupNode
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.model.ModelSkeleton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoseSkeletonsTest {

    @Test
    fun inferredForestKeepsParentChildEdges() {
        val hand = VertexGroupNode(2, intArrayOf(4))
        val arm = VertexGroupNode(1, intArrayOf(2, 3), listOf(hand))
        val body = VertexGroupNode(0, intArrayOf(0, 1), listOf(arm))

        val skeleton = PoseSkeletons.toPoseSkeleton(listOf(body))

        assertEquals(listOf(0, 1, 2), skeleton.bones.map { it.groupIndex })
        assertNull(skeleton.bone(0)?.parentGroupIndex)
        assertEquals(0, skeleton.bone(1)?.parentGroupIndex)
        assertEquals(1, skeleton.bone(2)?.parentGroupIndex)
        assertEquals(listOf(0, 1, 2), skeleton.bone(0)?.descendantGroups?.toList())
        assertEquals(listOf(2), skeleton.bone(2)?.descendantGroups?.toList())
    }

    @Test
    fun framemapNestingBecomesVertexGroupParents() {
        val body = VertexGroupNode(0, intArrayOf(0, 1))
        val hand = VertexGroupNode(2, intArrayOf(4))
        val arm = TransformHierarchyNode(
            1,
            TransformationType.ROTATE,
            listOf(1, 2),
            listOf(VertexGroupNode(1, intArrayOf(2, 3)), hand)
        )
        val root = TransformHierarchyNode(
            0,
            TransformationType.SET_OFFSET,
            listOf(0, 1, 2),
            listOf(body, arm)
        )

        val skeleton = PoseSkeletons.fromHierarchy(listOf(root))

        assertNull(skeleton.bone(0)?.parentGroupIndex)
        assertEquals(0, skeleton.bone(1)?.parentGroupIndex)
        assertEquals(0, skeleton.bone(2)?.parentGroupIndex)
        assertTrue(skeleton.bone(0)!!.descendantGroups.toList().containsAll(listOf(0, 1, 2)))
    }

    @Test
    fun framemapWithOnlyNestedTransformsStillParentsLimbs() {
        val torso = TransformHierarchyNode(
            1,
            TransformationType.ROTATE,
            listOf(0),
            listOf(VertexGroupNode(0, intArrayOf(0, 1)))
        )
        val hand = TransformHierarchyNode(
            3,
            TransformationType.ROTATE,
            listOf(2),
            listOf(VertexGroupNode(2, intArrayOf(4)))
        )
        val arm = TransformHierarchyNode(
            2,
            TransformationType.ROTATE,
            listOf(1, 2),
            listOf(VertexGroupNode(1, intArrayOf(2, 3)), hand)
        )
        val root = TransformHierarchyNode(
            0,
            TransformationType.SET_OFFSET,
            listOf(0, 1, 2),
            listOf(torso, arm)
        )
        val sizes = mapOf(0 to 2, 1 to 2, 2 to 1)
        val skeleton = PoseSkeletons.fromHierarchy(listOf(root)) { sizes[it] ?: 0 }

        assertNull(skeleton.bone(0)?.parentGroupIndex)
        assertEquals(0, skeleton.bone(1)?.parentGroupIndex)
        assertEquals(1, skeleton.bone(2)?.parentGroupIndex)
    }

    @Test
    fun bucketsAreUnwrapped() {
        val leaf = VertexGroupNode(3, intArrayOf(1))
        val bucket = BucketHierarchyNode("Ungrouped", listOf(leaf), listOf(3))
        val skeleton = PoseSkeletons.fromHierarchy(listOf(bucket))
        assertEquals(listOf(3), skeleton.bones.map { it.groupIndex })
        assertNull(skeleton.bone(3)?.parentGroupIndex)
    }

    @Test
    fun spatialSkeletonParentsSmallerSkinsToTheLargest() {
        val skeleton = ModelSkeleton(limbModel())
        val pose = PoseSkeletons.spatialSkeleton(skeleton)
        val root = pose.bones.single { it.parentGroupIndex == null }.groupIndex
        assertEquals(0, root)
        assertTrue(pose.bones.any { it.parentGroupIndex == 0 })
    }

    @Test
    fun proximalJointSitsCloserToTheParentThanTheCentroid() {
        val model = ModelSkeleton(limbModel())
        val pose = PoseSkeleton(
            listOf(
                PoseBone(0, null, intArrayOf(0, 1)),
                PoseBone(1, 0, intArrayOf(1)),
            )
        )
        val centroid = model.groupCentroid(1)!!
        val joint = PoseSkeletons.jointPosition(model, pose, 1)!!
        val parent = model.groupCentroid(0)!!
        assertTrue(distance(joint, parent) < distance(centroid, parent) - 1e-3)
        assertTrue(joint.first < centroid.first)
    }

    @Test
    fun proximalJointOnATwoVertexSkinDoesNotThrow() {
        val model = ModelSkeleton(
            QodatModelDefinition(
                name = "tiny",
                vertexCount = 3,
                vertexPositionsX = intArrayOf(0, 16, 32),
                vertexPositionsY = intArrayOf(0, 0, 0),
                vertexPositionsZ = intArrayOf(0, 0, 0),
                vertexSkins = intArrayOf(0, 1, 1),
                faceCount = 1,
                faceVertexIndices1 = intArrayOf(0),
                faceVertexIndices2 = intArrayOf(1),
                faceVertexIndices3 = intArrayOf(2),
                faceSkins = intArrayOf(0),
                faceAlphas = null,
                facePriorities = null,
                faceTypes = null,
                faceColors = shortArrayOf(0),
            )
        )
        val pose = PoseSkeleton(
            listOf(
                PoseBone(0, null, intArrayOf(0, 1)),
                PoseBone(1, 0, intArrayOf(1)),
            )
        )
        val joint = PoseSkeletons.jointPosition(model, pose, 1)
        assertTrue(joint != null)
    }

    private fun limbModel() = QodatModelDefinition(
        name = "limb",
        vertexCount = 11,
        vertexPositionsX = intArrayOf(0, 8, 16, 24, 32, 40, 80, 100, 140, 0, 16),
        vertexPositionsY = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        vertexPositionsZ = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 64),
        vertexSkins = intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2),
        faceCount = 2,
        faceVertexIndices1 = intArrayOf(0, 9),
        faceVertexIndices2 = intArrayOf(1, 10),
        faceVertexIndices3 = intArrayOf(5, 9),
        faceSkins = intArrayOf(0, 2),
        faceAlphas = null,
        facePriorities = null,
        faceTypes = null,
        faceColors = shortArrayOf(0, 0),
    )

    private fun distance(
        a: Triple<Double, Double, Double>,
        b: Triple<Double, Double, Double>
    ): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        val dz = a.third - b.third
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
