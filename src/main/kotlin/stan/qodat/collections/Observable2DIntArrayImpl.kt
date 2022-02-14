package stan.qodat.collections

import javafx.collections.ObservableArrayBase
import java.util.*
import kotlin.math.min

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class Observable2DIntArrayImpl(private var size: Int = 0) :
    ObservableArrayBase<Observable2DIntArray>(),
    Observable2DIntArray
{
    private var array = INITIAL

    constructor(array: Array<IntArray>) : this() {
        setAllInternal(array, 0, array.size)
    }

    override fun get(index: Int): IntArray {
        rangeCheck(index + 1)
        return array[index]!!
    }

    override fun setAll(vararg elements: IntArray) {
        setAllInternal(arrayOf(*elements), 0, elements.size)
    }

    private fun setAllInternal(src: Array<IntArray>, srcIndex: Int, length: Int) {
        val sizeChanged = size() != length
        size = 0
        ensureCapacity(length)
        System.arraycopy(src, srcIndex, array, 0, length)
        size = length
        fireChange(sizeChanged, 0, size)
    }

    override fun set(index: Int, value: IntArray) {
        rangeCheck(index + 1)
        array[index] = value
        fireChange(false, index, index + 1)
    }

    override fun toArray(dest: Array<IntArray?>): Array<IntArray?> {
        var safeDest = dest
        if (size() > safeDest.size)
            safeDest = arrayOfNulls<IntArray?>(size())
        System.arraycopy(array, 0, safeDest, 0, size())
        return safeDest
    }

    private fun rangeCheck(size: Int) {
        if (size > this.size)
            throw ArrayIndexOutOfBoundsException(this.size)
    }

    override fun resize(newSize: Int) {
        if (newSize < 0)
            throw NegativeArraySizeException("Can't resize to negative value: $newSize")
        ensureCapacity(newSize)
        val minSize = min(size, newSize)
        val sizeChanged = size != newSize
        size = newSize
        Arrays.fill(array, minSize, size, IntArray(0))
        fireChange(sizeChanged, minSize, newSize)
    }

    override fun ensureCapacity(capacity: Int) {
        if (array.size < capacity)
            array = array.copyOf(capacity)
    }

    override fun trimToSize() {
        if (array.size != size) {
            val newArray = arrayOfNulls<IntArray>(size)
            System.arraycopy(array, 0, newArray, 0, size)
            array = newArray
        }
    }

    override fun clear() {
        resize(0)
    }

    override fun size() = size

    companion object {
        private val INITIAL = emptyArray<IntArray?>()
    }
}