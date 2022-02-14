package stan.qodat.collections

import javafx.collections.ObservableArray

/**
 * Represents an [ObservableArray] of [int arrays][IntArray].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
interface Observable2DIntArray : ObservableArray<Observable2DIntArray> {

    /**
     * Gets the [element][IntArray] at the [index] of this array.
     *
     * @throws ArrayIndexOutOfBoundsException if [index] is outside of array bounds
     */
    operator fun get(index: Int): IntArray

    /**
     * Sets the [value] the [index] in this array.
     *
     * @throws ArrayIndexOutOfBoundsException if `index` is outside of array bounds
     */
    operator fun set(index: Int, value: IntArray)

    /**
     * Replaces this array's content with [elements].
     *
     * @throws NullPointerException if [elements] is null
     */
    fun setAll(vararg elements: IntArray)

    /**
     * Returns a copy of this array and puts it in [destination] or if it is too small,
     * create a new array to put the copy in.
     *
     * @return an array of [int arrays][IntArray]
     */
    fun toArray(destination: Array<IntArray?>): Array<IntArray?>
}