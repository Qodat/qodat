package stan.qodat.util

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   23/09/2019
 * @version 1.0
 */
interface Action {

    fun action()

    fun undoAction()
}