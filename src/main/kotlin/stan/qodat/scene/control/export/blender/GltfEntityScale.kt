package stan.qodat.scene.control.export.blender

import qodat.cache.definition.EntityDefinition
import qodat.cache.definition.ItemDefinition
import qodat.cache.definition.NPCDefinition
import qodat.cache.definition.ObjectDefinition
import qodat.cache.definition.SpotAnimationDefinition

/**
 * Client model resize (`RSModelData.resize`): each axis is multiplied by `n / 128`.
 * NPC width applies to X and Z; height applies to Y.
 */
data class GltfEntityScale(
    val x: Int = IDENTITY_AXIS,
    val y: Int = IDENTITY_AXIS,
    val z: Int = IDENTITY_AXIS,
) {
    fun factorX() = x / IDENTITY_AXIS.toFloat()
    fun factorY() = y / IDENTITY_AXIS.toFloat()
    fun factorZ() = z / IDENTITY_AXIS.toFloat()

    companion object {
        const val IDENTITY_AXIS = 128
        val IDENTITY = GltfEntityScale()

        fun from(definition: EntityDefinition): GltfEntityScale = when (definition) {
            is NPCDefinition -> GltfEntityScale(definition.widthScale, definition.heightScale, definition.widthScale)
            is ObjectDefinition -> GltfEntityScale(
                definition.modelSizeX,
                definition.modelSizeHeight,
                definition.modelSizeY,
            )
            is ItemDefinition -> GltfEntityScale(definition.resizeX, definition.resizeY, definition.resizeZ)
            is SpotAnimationDefinition -> GltfEntityScale(definition.resizeX, definition.resizeY, definition.resizeX)
            else -> IDENTITY
        }
    }
}
