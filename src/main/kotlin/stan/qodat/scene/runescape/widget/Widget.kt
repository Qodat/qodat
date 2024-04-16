package stan.qodat.scene.runescape.widget

import stan.qodat.scene.runescape.widget.component.Component
import stan.qodat.scene.runescape.widget.component.Pos
import stan.qodat.scene.runescape.widget.component.Size
import stan.qodat.scene.runescape.widget.component.WidgetRef
import stan.qodat.scene.runescape.widget.component.impl.Layer
import kotlin.reflect.KProperty

class Widget {

    val children = mutableListOf<Component<*>>()

    operator fun getValue(any: Any?, property: KProperty<*>) =
        WidgetRef(property.name, this)

}
