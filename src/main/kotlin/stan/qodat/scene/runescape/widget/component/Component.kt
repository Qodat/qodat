package stan.qodat.scene.runescape.widget.component

import kotlin.reflect.KProperty

abstract class Component<R : Ref<*>> {

    abstract var name: String?
    abstract var x: Int
    abstract var y: Int
    abstract var width: Int
    abstract var height: Int
    abstract var hSize: Size
    abstract var vSize: Size
    abstract var hPos: Pos
    abstract var vPos: Pos

    var hidden: Boolean = false

    var buttonType: ButtonType? = null
    var parent: Component<*>? = null

    operator fun getValue(any: Any?, property: KProperty<*>) : R {
        name = property.name
        return toReference(property.name)
    }

    abstract fun toReference(name: String): R

    override fun toString(): String =
        name?.let { "${javaClass.simpleName}:$it" }?:super.toString()
}
