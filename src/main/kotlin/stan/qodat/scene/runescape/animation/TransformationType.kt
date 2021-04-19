package stan.qodat.scene.runescape.animation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
enum class TransformationType {

    SET_OFFSET,
    TRANSLATE,
    ROTATE,
    SCALE,
    TRANSPARENCY,
    UNDEFINED;

    companion object {
        fun get(int: Int): TransformationType {
            return if (int >= 0 && int < values().size)
                values()[int]
            else
                UNDEFINED
        }
    }
}