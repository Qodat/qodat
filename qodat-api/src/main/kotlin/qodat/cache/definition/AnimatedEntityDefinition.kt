package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface AnimatedEntityDefinition : EntityDefinition {

    val animationIds: Array<String>

    /**
     * Stance / walk / turn ids that should resolve immediately.
     * Extra skeleton-matched ids (humanoids) stay in [animationIds] and load on demand.
     */
    val primaryAnimationIds: Array<String>
        get() = animationIds
}