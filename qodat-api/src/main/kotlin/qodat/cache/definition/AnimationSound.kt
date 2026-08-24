package qodat.cache.definition

/**
 * Sequence / Maya frame sound. Fields match RuneLite
 * `SequenceDefinition.Sound` 1:1 (`id`, `loops`, `location`, `retain`, `weight`).
 */
data class AnimationSound(
    val id: Int,
    val loops: Int,
    val location: Int,
    val retain: Int,
    val weight: Int = -1,
)
