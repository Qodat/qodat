package stan.qodat.collections

import javafx.collections.ObservableArray

/**
 * Represents an [ObservableArray] of [float arrays][IntArray].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
interface ObservableFaceArrayArray : ObservableArray<ObservableFaceArrayArray> {

    /**
     * Gets a single value of array. This is generally as fast as direct access
     * to an array and eliminates necessity to make a copy of array.
     * @param index index of element to get
     * @return [IntArray] at the given index
     * @throws ArrayIndexOutOfBoundsException if `index` is outside array bounds
     */
    operator fun get(index: Int): IntArray

    /**
     * Replaces this observable array content with given elements.
     * Capacity is increased if necessary to match the new size of the data.
     * @param elements elements to put into array content
     * @throws NullPointerException if [elements] is null
     */
    fun setAll(vararg elements: IntArray)

    /**
     * Sets a single value in the array. Avoid using this method if many values
     * are updated, use [.setAll] update method
     * instead with as minimum number of invocations as possible.
     * @param index index of the value to set
     * @param value new value for the given index
     * @throws ArrayIndexOutOfBoundsException if `index` is outside
     * array bounds
     */
    operator fun set(index: Int, value: IntArray)

    /**
     * Returns an array containing copy of the observable array.
     * If the observable array fits in the specified array, it is copied therein.
     * Otherwise, a new array is allocated with the size of the observable array.
     *
     * @param dest  the array into which the observable array to be copied,
     *              if it is big enough; otherwise, a new array of float arrays is allocated.
     *
     * @return an array of float arrays containing the copy of the observable array
     */
    fun toArray(dest: Array<IntArray?>): Array<IntArray?>

    operator fun iterator(): Iterator<IntArray>
}