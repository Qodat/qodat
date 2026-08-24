package stan.qodat.scene.control.tree

import stan.qodat.scene.runescape.animation.TransformationType
import kotlin.test.Test
import kotlin.test.assertEquals

class VertexGroupHierarchyTest {

    @Test
    fun vertexGroupHighlightsSelfAndNestedDescendants() {
        val leaf = VertexGroupNode(3, intArrayOf(7, 8))
        val branch = VertexGroupNode(1, intArrayOf(1, 2), listOf(leaf, VertexGroupNode(2, intArrayOf(4))))
        assertEquals(listOf(3), leaf.highlightGroups)
        assertEquals(listOf(1, 3, 2), branch.highlightGroups)
    }

    @Test
    fun transformNodeHighlightsTargetGroupsOnly() {
        val child = VertexGroupNode(9, intArrayOf(0))
        val node = TransformHierarchyNode(0, TransformationType.TRANSLATE, listOf(4, 5), listOf(child))
        assertEquals(listOf(4, 5), node.highlightGroups)
        assertEquals(listOf(9), child.highlightGroups)
    }

    @Test
    fun bucketNodeUsesExplicitHighlightIdentity() {
        val node = BucketHierarchyNode(
            "Ungrouped",
            listOf(VertexGroupNode(2, intArrayOf(1)), VertexGroupNode(4, intArrayOf(3))),
            listOf(2, 4)
        )
        assertEquals(listOf(2, 4), node.highlightGroups)
        assertEquals("Ungrouped", node.title)
    }
}
