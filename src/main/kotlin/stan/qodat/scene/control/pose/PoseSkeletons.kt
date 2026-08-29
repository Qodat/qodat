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
import kotlin.math.sqrt

object PoseSkeletons {

    fun resolve(model: ModelSkeleton): PoseSkeleton {
        val skeletons = resolveSkeletons()
        if (skeletons.isNotEmpty()) {
            val fromFramemap = fromHierarchy(
                VertexGroupHierarchy.build(model, skeletons).second
            ) { group -> model.getVertexGroups().getOrNull(group)?.size ?: 0 }
            if (fromFramemap.bones.any { it.parentGroupIndex != null })
                return fromFramemap
        }
        return spatialSkeleton(model)
    }

    fun resolveSkeletons(): List<Pair<Int, AnimationSkeleton>> {
        val entity = Properties.selectedEntity.get() as? AnimatedEntity<*>
        val animation = Properties.selectedAnimation.get()
            ?: entity?.selectedAnimation?.get()
            ?: entity?.getPrimaryAnimations()?.firstOrNull { it is AnimationLegacy }
            ?: entity?.getAnimations()?.firstOrNull { it is AnimationLegacy }
            ?: return emptyList()
        if (animation !is AnimationLegacy)
            return emptyList()
        return animation.getSkeletons().entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    /**
     * Keep framemap nesting as vertex-group parents. A SET_OFFSET / ROTATE
     * node often has no exclusive leaves (every skin sits on a deeper
     * transform); parent nested groups to that node's largest target skin
     * so the tree does not collapse into isolated roots.
     */
    internal fun fromHierarchy(
        nodes: List<VertexHierarchyNode>,
        groupSize: (Int) -> Int = { 0 }
    ): PoseSkeleton {
        val parentOf = LinkedHashMap<Int, Int?>()
        fun add(group: Int, parent: Int?) {
            if (group !in parentOf)
                parentOf[group] = parent
        }
        fun walk(node: VertexHierarchyNode, parent: Int?) {
            when (node) {
                is VertexGroupNode -> {
                    add(node.index, parent)
                    for (child in node.children)
                        walk(child, node.index)
                }
                is TransformHierarchyNode -> {
                    val exclusive = node.children.filterIsInstance<VertexGroupNode>()
                    val nested = node.children.filter { it !is VertexGroupNode }
                    for (group in exclusive)
                        walk(group, parent)
                    val nextParent = exclusive.maxByOrNull { it.vertices.size }?.index
                        ?: node.targetGroups.maxByOrNull(groupSize)
                    if (nextParent != null)
                        add(nextParent, parent)
                    val childParent = nextParent ?: parent
                    for (child in nested)
                        walk(child, childParent)
                }
                is BucketHierarchyNode -> {
                    for (child in node.children)
                        walk(child, parent)
                }
            }
        }
        for (root in nodes)
            walk(root, null)
        return skeletonFromParents(parentOf)
    }

    internal fun toPoseSkeleton(roots: List<VertexGroupNode>): PoseSkeleton {
        val parentOf = LinkedHashMap<Int, Int?>()
        fun walk(node: VertexGroupNode, parent: Int?) {
            parentOf[node.index] = parent
            for (child in node.children)
                walk(child, node.index)
        }
        for (root in roots)
            walk(root, null)
        return skeletonFromParents(parentOf)
    }

    /**
     * Prim MST from the largest skin toward nearby skins. Used when no
     * framemap is loaded — closer to modelkit's named armature than a
     * face-adjacency BFS, which snakes through the mesh.
     */
    internal fun spatialSkeleton(model: ModelSkeleton): PoseSkeleton {
        val groups = model.getVertexGroups().withIndex()
            .filter { it.value.isNotEmpty() }
            .map { it.index }
        if (groups.isEmpty())
            return PoseSkeleton(emptyList())
        val centroids = HashMap<Int, Triple<Double, Double, Double>>()
        for (group in groups) {
            val centroid = model.groupCentroid(group) ?: continue
            centroids[group] = centroid
        }
        val live = groups.filter { it in centroids }
        if (live.isEmpty())
            return PoseSkeleton(emptyList())
        val root = live.maxWith(compareBy<Int> { model.getVertexGroups()[it].size }.thenBy { it })
        val parentOf = LinkedHashMap<Int, Int?>()
        parentOf[root] = null
        val assigned = hashSetOf(root)
        while (assigned.size < live.size) {
            var bestChild = -1
            var bestParent = -1
            var bestDist = Double.POSITIVE_INFINITY
            for (child in live) {
                if (child in assigned)
                    continue
                val childC = centroids.getValue(child)
                for (parent in assigned) {
                    val dist = distance(childC, centroids.getValue(parent))
                    if (dist < bestDist) {
                        bestDist = dist
                        bestChild = child
                        bestParent = parent
                    }
                }
            }
            if (bestChild < 0)
                break
            parentOf[bestChild] = bestParent
            assigned += bestChild
        }
        for (group in live) {
            if (group !in parentOf)
                parentOf[group] = null
        }
        return skeletonFromParents(parentOf)
    }

    /**
     * Modelkit `proximal_joints`: sit the joint on the parent-facing edge of
     * the skin, not the volume centroid (which hides inside the mesh).
     */
    fun jointPosition(model: ModelSkeleton, skeleton: PoseSkeleton, groupIndex: Int): Triple<Double, Double, Double>? {
        val bone = skeleton.bone(groupIndex) ?: return model.groupCentroid(groupIndex)
        val vertices = model.getVertexGroups().getOrNull(groupIndex) ?: return null
        if (vertices.isEmpty())
            return null
        val parent = bone.parentGroupIndex
        if (parent == null)
            return model.groupCentroid(groupIndex)
        val parentJoint = jointPosition(model, skeleton, parent) ?: return model.groupCentroid(groupIndex)
        return proximalMean(model, vertices, parentJoint)
    }

    internal fun skeletonFromParents(parentOf: Map<Int, Int?>): PoseSkeleton {
        val children = HashMap<Int, MutableList<Int>>()
        for ((group, parent) in parentOf) {
            if (parent != null)
                children.getOrPut(parent) { ArrayList() }.add(group)
        }
        fun descendants(group: Int): IntArray {
            val out = ArrayList<Int>()
            fun walk(current: Int) {
                out += current
                for (child in children[current].orEmpty())
                    walk(child)
            }
            walk(group)
            return out.toIntArray()
        }
        val order = ArrayList<Int>()
        val remaining = parentOf.keys.toMutableSet()
        while (remaining.isNotEmpty()) {
            val next = remaining.firstOrNull { parentOf[it] !in remaining } ?: remaining.first()
            order += next
            remaining -= next
        }
        return PoseSkeleton(
            order.map { group -> PoseBone(group, parentOf[group], descendants(group)) }
        )
    }

    private fun proximalMean(
        model: ModelSkeleton,
        vertices: IntArray,
        parentJoint: Triple<Double, Double, Double>
    ): Triple<Double, Double, Double> {
        val keep = minOf(vertices.size, maxOf(3, vertices.size / 10))
        val nearest = vertices
            .map { vertex ->
                val x = model.getX(vertex).toDouble()
                val y = model.getY(vertex).toDouble()
                val z = model.getZ(vertex).toDouble()
                val dx = x - parentJoint.first
                val dy = y - parentJoint.second
                val dz = z - parentJoint.third
                (dx * dx + dy * dy + dz * dz) to Triple(x, y, z)
            }
            .sortedBy { it.first }
            .take(keep)
        var sx = 0.0
        var sy = 0.0
        var sz = 0.0
        for ((_, point) in nearest) {
            sx += point.first
            sy += point.second
            sz += point.third
        }
        val n = nearest.size.toDouble()
        return Triple(sx / n, sy / n, sz / n)
    }

    private fun distance(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        val dz = a.third - b.third
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
