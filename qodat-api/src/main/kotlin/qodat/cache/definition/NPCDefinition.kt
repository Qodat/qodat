package qodat.cache.definition

interface NPCDefinition : AnimatedEntityDefinition {

    /** Client X/Z resize. Identity is 128 (`RSModelData.resize`). */
    val widthScale: Int get() = 128

    /** Client Y resize. Identity is 128. */
    val heightScale: Int get() = 128
}
