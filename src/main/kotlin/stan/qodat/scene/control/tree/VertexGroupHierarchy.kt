package stan.qodat.scene.control.tree

import stan.qodat.scene.runescape.animation.AnimationSkeleton
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.model.Model

internal sealed class VertexHierarchyNode {
    abstract val highlightGroups: List<Int>
}

internal class TransformHierarchyNode(
    val index: Int,
    val type: TransformationType,
    val targetGroups: List<Int>,
    val children: List<VertexHierarchyNode>
) : VertexHierarchyNode() {
    override val highlightGroups: List<Int> = targetGroups
}

internal class VertexGroupNode(
    val index: Int,
    val vertices: IntArray,
    val children: List<VertexGroupNode> = emptyList()
) : VertexHierarchyNode() {
    override val highlightGroups: List<Int> =
        listOf(index) + children.flatMap { it.highlightGroups }
}

internal class BucketHierarchyNode(
    val title: String,
    val children: List<VertexHierarchyNode>,
    override val highlightGroups: List<Int>
) : VertexHierarchyNode()

internal object VertexGroupHierarchy {

    fun build(
        model: Model,
        skeletons: List<Pair<Int, AnimationSkeleton>>
    ): Pair<String, List<VertexHierarchyNode>> {
        if (skeletons.isEmpty())
            return "Vertex Group Hierarchy (inferred)" to buildInferred(model)
        val roots = ArrayList<VertexHierarchyNode>()
        if (skeletons.size == 1) {
            roots += buildFromSkeleton(model, skeletons[0].second)
        } else {
            for ((id, skeleton) in skeletons) {
                val children = buildFromSkeleton(model, skeleton)
                if (children.isEmpty())
                    continue
                roots += BucketHierarchyNode(
                    "SKELETON $id",
                    children,
                    children.flatMap { it.highlightGroups }.distinct()
                )
            }
        }
        val ungrouped = ungroupedLeaves(model, skeletons)
        if (ungrouped.isNotEmpty()) {
            roots += BucketHierarchyNode(
                "Ungrouped",
                ungrouped,
                ungrouped.map { it.index }
            )
        }
        return "Vertex Group Hierarchy" to roots
    }

    private fun buildFromSkeleton(model: Model, skeleton: AnimationSkeleton): List<VertexHierarchyNode> {
        val vertexGroups = model.getVertexGroups()
        val sets = skeleton.getTransformationGroups().mapIndexedNotNull { index, transform ->
            val groups = transform.groupIndices
                .filter { vertexGroups.getOrNull(it)?.isNotEmpty() == true }
                .distinct()
                .sorted()
            if (groups.isEmpty())
                null
            else
                TransformSet(
                    index = index,
                    type = transform.typeProperty.get(),
                    groups = groups,
                    vertices = groups.flatMap { vertexGroups[it].asIterable() }.toHashSet()
                )
        }
        if (sets.isEmpty())
            return emptyList()

        // Parent = smallest proper vertex-set superset; equal sets nest under the lower index.
        val parentByIndex = HashMap<Int, Int>()
        for (a in sets) {
            val equalParent = sets
                .filter { it.index < a.index && it.vertices == a.vertices }
                .minByOrNull { it.index }
            if (equalParent != null) {
                parentByIndex[a.index] = equalParent.index
                continue
            }
            val parent = sets
                .filter { it.vertices.size > a.vertices.size && it.vertices.containsAll(a.vertices) }
                .minWithOrNull(compareBy({ it.vertices.size }, { it.index }))
            if (parent != null)
                parentByIndex[a.index] = parent.index
        }

        val childrenOf = HashMap<Int, MutableList<Int>>()
        val roots = ArrayList<Int>()
        for (set in sets) {
            val parent = parentByIndex[set.index]
            if (parent == null)
                roots += set.index
            else
                childrenOf.getOrPut(parent) { ArrayList() }.add(set.index)
        }
        roots.sort()
        childrenOf.values.forEach { it.sort() }

        val byIndex = sets.associateBy { it.index }
        val deepestForGroup = HashMap<Int, Int>()
        val allGroups = sets.flatMap { it.groups }.toSortedSet()
        for (group in allGroups) {
            val containers = sets.filter { group in it.groups }
            val deepest = containers.minWith(
                compareBy<TransformSet> { it.vertices.size }.thenByDescending { it.index }
            )
            deepestForGroup[group] = deepest.index
        }

        fun buildNode(index: Int): TransformHierarchyNode {
            val set = byIndex.getValue(index)
            val nested = childrenOf[index].orEmpty().map { buildNode(it) }
            val leaves = deepestForGroup
                .filter { it.value == index }
                .keys
                .sorted()
                .map { VertexGroupNode(it, vertexGroups[it]) }
            return TransformHierarchyNode(set.index, set.type, set.groups, nested + leaves)
        }

        return roots.map { buildNode(it) }.filter { it.children.isNotEmpty() }
    }

    private fun ungroupedLeaves(
        model: Model,
        skeletons: List<Pair<Int, AnimationSkeleton>>
    ): List<VertexGroupNode> {
        val targeted = HashSet<Int>()
        for ((_, skeleton) in skeletons) {
            for (transform in skeleton.getTransformationGroups())
                targeted.addAll(transform.groupIndices)
        }
        return model.getVertexGroups().withIndex()
            .filter { (index, vertices) -> vertices.isNotEmpty() && index !in targeted }
            .map { VertexGroupNode(it.index, it.value) }
    }

    private fun buildInferred(model: Model): List<VertexHierarchyNode> {
        val groups = model.getVertexGroups()
        val nonEmpty = groups.withIndex()
            .filter { it.value.isNotEmpty() }
            .map { it.index }
            .sorted()
        if (nonEmpty.isEmpty())
            return emptyList()
        val nonEmptySet = nonEmpty.toHashSet()
        val adjacency = faceAdjacency(model, groups.size, nonEmptySet)
        val bounds = nonEmpty.associateWith { groupBounds(model, groups[it]) }
        val visited = HashSet<Int>()
        val roots = ArrayList<VertexGroupNode>()
        while (true) {
            val start = nonEmpty
                .filter { it !in visited }
                .maxWithOrNull(compareBy<Int> { groups[it].size }.thenBy { -it })
                ?: break
            roots += spanningTree(start, groups, adjacency, bounds, nonEmptySet, visited)
        }
        return roots
    }

    // Face-adjacent groups form a connectivity graph; BFS from the largest
    // group yields a deterministic limb-like tree (smaller / contained neighbors first).
    private fun spanningTree(
        root: Int,
        groups: Array<IntArray>,
        adjacency: Array<out Set<Int>>,
        bounds: Map<Int, GroupBounds>,
        nonEmpty: Set<Int>,
        visited: MutableSet<Int>
    ): VertexGroupNode {
        val childrenOf = HashMap<Int, MutableList<Int>>()
        val queue = ArrayDeque<Int>()
        visited.add(root)
        queue.add(root)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentBounds = bounds[current]
            val neighbors = adjacency[current]
                .asSequence()
                .filter { it in nonEmpty && it !in visited }
                .sortedWith(
                    compareBy<Int> { neighbor ->
                        val contained = currentBounds != null &&
                            bounds[neighbor]?.let { currentBounds.contains(it) } == true
                        if (contained) 0 else 1
                    }.thenByDescending { groups[it].size }.thenBy { it }
                )
                .toList()
            for (neighbor in neighbors) {
                if (!visited.add(neighbor))
                    continue
                childrenOf.getOrPut(current) { ArrayList() }.add(neighbor)
                queue.add(neighbor)
            }
        }
        fun node(index: Int): VertexGroupNode =
            VertexGroupNode(
                index,
                groups[index],
                childrenOf[index].orEmpty().map { node(it) }
            )
        return node(root)
    }

    private fun faceAdjacency(model: Model, groupCount: Int, nonEmpty: Set<Int>): Array<Set<Int>> {
        val skins = vertexToGroup(model)
        val adjacent = Array(groupCount) { HashSet<Int>() }
        val definition = model.modelDefinition
        val v1 = definition.getFaceVertexIndices1()
        val v2 = definition.getFaceVertexIndices2()
        val v3 = definition.getFaceVertexIndices3()
        for (face in 0 until definition.getFaceCount()) {
            val a = skins.getOrNull(v1[face]) ?: continue
            val b = skins.getOrNull(v2[face]) ?: continue
            val c = skins.getOrNull(v3[face]) ?: continue
            link(adjacent, nonEmpty, a, b)
            link(adjacent, nonEmpty, a, c)
            link(adjacent, nonEmpty, b, c)
        }
        return Array(groupCount) { adjacent[it] }
    }

    private fun link(adjacent: Array<HashSet<Int>>, nonEmpty: Set<Int>, a: Int, b: Int) {
        if (a == b || a !in nonEmpty || b !in nonEmpty)
            return
        adjacent[a].add(b)
        adjacent[b].add(a)
    }

    private fun vertexToGroup(model: Model): IntArray {
        val skins = model.modelDefinition.getVertexSkins()
        if (skins != null)
            return skins
        val map = IntArray(model.getVertexCount()) { -1 }
        for ((group, vertices) in model.getVertexGroups().withIndex()) {
            for (vertex in vertices)
                if (vertex in map.indices)
                    map[vertex] = group
        }
        return map
    }

    private fun groupBounds(model: Model, vertices: IntArray): GroupBounds {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (vertex in vertices) {
            val x = model.getX(vertex)
            val y = model.getY(vertex)
            val z = model.getZ(vertex)
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }
        return GroupBounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    private data class TransformSet(
        val index: Int,
        val type: TransformationType,
        val groups: List<Int>,
        val vertices: Set<Int>
    )

    private data class GroupBounds(
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int,
        val minZ: Int,
        val maxZ: Int
    ) {
        fun contains(other: GroupBounds): Boolean =
            minX <= other.minX && maxX >= other.maxX &&
                minY <= other.minY && maxY >= other.maxY &&
                minZ <= other.minZ && maxZ >= other.maxZ &&
                (minX != other.minX || maxX != other.maxX ||
                    minY != other.minY || maxY != other.maxY ||
                    minZ != other.minZ || maxZ != other.maxZ)
    }
}