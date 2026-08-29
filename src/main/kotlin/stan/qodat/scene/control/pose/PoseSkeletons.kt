package stan.qodat.scene.control.pose

import stan.qodat.Properties
import stan.qodat.scene.control.tree.BucketHierarchyNode
import stan.qodat.scene.control.tree.TransformHierarchyNode
import stan.qodat.scene.control.tree.VertexGroupHierarchy
import stan.qodat.scene.control.tree.VertexGroupNode
import stan.qodat.scene.control.tree.VertexHierarchyNode
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.animation.AnimationSkeleton
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.ModelSkeleton

object PoseSkeletons {

    fun resolve(model: ModelSkeleton): PoseSkeleton {
        val fromAnimation = vertexGroupForest(VertexGroupHierarchy.build(model, resolveSkeletons()).second)
        if (fromAnimation.any { it.children.isNotEmpty() })
            return toPoseSkeleton(fromAnimation)
        val inferred = vertexGroupForest(VertexGroupHierarchy.build(model, emptyList()).second)
        return toPoseSkeleton(inferred)
    }

    fun resolveSkeletons(): List<Pair<Int, AnimationSkeleton>> {
        val animation = Properties.selectedAnimation.get()
            ?: (Properties.selectedEntity.get() as? AnimatedEntity<*>)?.selectedAnimation?.get()
            ?: return emptyList()
        if (animation !is AnimationLegacy)
            return emptyList()
        return animation.getSkeletons().entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    internal fun vertexGroupForest(nodes: List<VertexHierarchyNode>): List<VertexGroupNode> {
        val out = ArrayList<VertexGroupNode>()
        for (node in nodes) {
            when (node) {
                is VertexGroupNode -> out += node
                is TransformHierarchyNode -> out += vertexGroupForest(node.children)
                is BucketHierarchyNode -> out += vertexGroupForest(node.children)
            }
        }
        return out
    }

    internal fun toPoseSkeleton(roots: List<VertexGroupNode>): PoseSkeleton {
        val bones = ArrayList<PoseBone>()
        fun walk(node: VertexGroupNode, parent: Int?) {
            bones += PoseBone(node.index, parent, node.highlightGroups.toIntArray())
            for (child in node.children)
                walk(child, node.index)
        }
        for (root in roots)
            walk(root, null)
        return PoseSkeleton(bones)
    }
}
