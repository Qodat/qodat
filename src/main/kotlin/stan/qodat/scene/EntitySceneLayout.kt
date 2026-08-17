package stan.qodat.scene

import javafx.scene.Group
import javafx.scene.Node
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

internal object EntitySceneLayout {

    const val TILE_SIZE = 128.0
    const val GAP = TILE_SIZE / 4.0

    fun apply(parent: Group, providersByNode: Map<Node, SceneNodeProvider>) {
        val items = parent.children.mapNotNull { node ->
            val provider = providersByNode[node] ?: return@mapNotNull null
            when (provider) {
                is Entity<*>, is Model -> layoutItem(node, provider as? Entity<*>)
                else -> null
            }
        }
        if (items.size <= 1) {
            items.forEach { reset(it.node) }
            return
        }

        val columns = ceil(sqrt(items.size.toDouble())).toInt().coerceAtLeast(1)
        val rows = items.chunked(columns)
        val rowDepths = rows.map { row -> row.maxOf { it.depth } }
        val rowWidths = rows.map { row ->
            row.sumOf { it.width } + GAP * (row.size - 1).coerceAtLeast(0)
        }
        val totalWidth = rowWidths.maxOrNull() ?: 0.0
        val totalDepth = rowDepths.sum() + GAP * (rows.size - 1).coerceAtLeast(0)
        val originX = -totalWidth / 2.0
        val originZ = -totalDepth / 2.0

        var z = originZ
        rows.forEachIndexed { rowIndex, row ->
            val rowWidth = rowWidths[rowIndex]
            var x = originX + (totalWidth - rowWidth) / 2.0
            for (item in row) {
                item.node.translateX = x + item.width / 2.0 - item.centerX
                item.node.translateZ = z + item.depth / 2.0 - item.centerZ
                x += item.width + GAP
            }
            z += rowDepths[rowIndex] + GAP
        }
    }

    private fun reset(node: Node) {
        node.translateX = 0.0
        node.translateZ = 0.0
    }

    private fun layoutItem(node: Node, entity: Entity<*>?): LayoutItem {
        val bounds = node.boundsInLocal
        val width = finiteExtent(bounds.width)
        val depth = finiteExtent(bounds.depth)
        val defTiles = entity?.let { tileSizeFromDefinition(it) } ?: 0
        val minExtent = defTiles * TILE_SIZE
        return LayoutItem(
            node = node,
            width = snapUp(max(width, minExtent)),
            depth = snapUp(max(depth, minExtent)),
            centerX = if (bounds.width.isFinite()) bounds.centerX else 0.0,
            centerZ = if (bounds.depth.isFinite()) bounds.centerZ else 0.0
        )
    }

    private fun finiteExtent(value: Double): Double =
        if (value.isFinite() && value > 0.0) value else TILE_SIZE

    private fun snapUp(value: Double): Double =
        ceil(value / TILE_SIZE).toInt().coerceAtLeast(1) * TILE_SIZE

    private fun tileSizeFromDefinition(entity: Entity<*>): Int {
        val definition = entity.definition
        for (name in SIZE_PROPERTY_NAMES) {
            val value = readIntProperty(definition, name)
            if (value != null && value > 0)
                return value
        }
        return 0
    }

    private fun readIntProperty(target: Any, name: String): Int? {
        val getter = "get${name.replaceFirstChar { it.uppercase() }}"
        val method = target.javaClass.methods.firstOrNull { it.name == getter || it.name == name }
            ?: return null
        return try {
            (method.invoke(target) as? Number)?.toInt()
        } catch (_: Exception) {
            null
        }
    }

    private data class LayoutItem(
        val node: Node,
        val width: Double,
        val depth: Double,
        val centerX: Double,
        val centerZ: Double
    )

    private val SIZE_PROPERTY_NAMES = arrayOf("size", "tileSpacesOccupied", "sizeX")
}
