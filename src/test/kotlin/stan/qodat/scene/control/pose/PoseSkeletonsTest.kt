package stan.qodat.scene.control.pose

import stan.qodat.scene.control.tree.BucketHierarchyNode
import stan.qodat.scene.control.tree.TransformHierarchyNode
import stan.qodat.scene.control.tree.VertexGroupNode
import stan.qodat.scene.runescape.animation.TransformationType
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
    fun transformHierarchyFlattensToVertexGroupLeaves() {
        val leafA = VertexGroupNode(4, intArrayOf(1))
        val leafB = VertexGroupNode(5, intArrayOf(2))
        val transform = TransformHierarchyNode(
            0,
            TransformationType.ROTATE,
            listOf(4, 5),
            listOf(leafA, leafB)
        )
        val forest = PoseSkeletons.vertexGroupForest(listOf(transform))
        assertEquals(listOf(4, 5), forest.map { it.index })
        assertTrue(forest.all { it.children.isEmpty() })
    }

    @Test
    fun bucketsAreUnwrapped() {
        val leaf = VertexGroupNode(3, intArrayOf(1))
        val bucket = BucketHierarchyNode("Ungrouped", listOf(leaf), listOf(3))
        val forest = PoseSkeletons.vertexGroupForest(listOf(bucket))
        assertEquals(listOf(3), forest.map { it.index })
    }
}
