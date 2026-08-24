package stan.qodat.scene.runescape.widget

import qodat.cache.definition.InterfaceDefinition

/**
 * Client-accurate widget size and position, matching Near Reality's deob
 * `class76.method3243` (size) and `class59.method1558` (position).
 *
 * Size modes: 0 absolute, 1 parent-minus, 2 16384ths of parent, 4 aspect.
 * Position modes: 0 abs start, 1 abs centre, 2 abs end, 3/4/5 16384ths variants.
 */
object WidgetLayout {

    const val CANVAS_WIDTH = 765
    const val CANVAS_HEIGHT = 503

    data class Box(val x: Int, val y: Int, val width: Int, val height: Int)

    fun layout(definition: InterfaceDefinition, parentWidth: Int, parentHeight: Int): Box {
        val size = size(
            parentWidth = parentWidth,
            parentHeight = parentHeight,
            originalWidth = definition.originalWidth,
            originalHeight = definition.originalHeight,
            widthMode = definition.widthMode,
            heightMode = definition.heightMode,
        )
        return Box(
            x = position(parentWidth, definition.originalX, size.first, definition.xPositionMode),
            y = position(parentHeight, definition.originalY, size.second, definition.yPositionMode),
            width = size.first,
            height = size.second,
        )
    }

    fun size(
        parentWidth: Int,
        parentHeight: Int,
        originalWidth: Int,
        originalHeight: Int,
        widthMode: Int,
        heightMode: Int,
    ): Pair<Int, Int> {
        var width = sized(parentWidth, originalWidth, widthMode)
        var height = sized(parentHeight, originalHeight, heightMode)
        if (widthMode == 4 && originalHeight != 0)
            width = height * originalWidth / originalHeight
        if (heightMode == 4 && originalWidth != 0)
            height = originalHeight * width / originalWidth
        return width to height
    }

    fun position(parent: Int, original: Int, size: Int, mode: Int): Int = when (mode) {
        1 -> original + (parent - size) / 2
        2 -> parent - size - original
        3 -> (original * parent) shr 14
        4 -> ((original * parent) shr 14) + (parent - size) / 2
        5 -> parent - size - ((original * parent) shr 14)
        else -> original
    }

    fun childId(packedId: Int): Int = packedId and 0xffff

    fun parentChildId(parentId: Int): Int = if (parentId == -1) -1 else parentId and 0xffff

    fun buildHierarchy(definitions: List<InterfaceDefinition>): List<HierarchyNode> {
        val nodes = definitions.associate { childId(it.id) to HierarchyNode(it) }
        val roots = ArrayList<HierarchyNode>()
        for (node in nodes.values) {
            val parentKey = parentChildId(node.definition.parentId)
            val parent = if (parentKey == -1) null else nodes[parentKey]
            if (parent == null || parent === node) {
                roots.add(node)
            } else {
                parent.children.add(node)
            }
        }
        return roots
    }

    private fun sized(parent: Int, original: Int, mode: Int): Int = when (mode) {
        1 -> parent - original
        2 -> (original * parent) shr 14
        else -> original
    }

    class HierarchyNode(val definition: InterfaceDefinition) {
        val children = ArrayList<HierarchyNode>()
    }
}
