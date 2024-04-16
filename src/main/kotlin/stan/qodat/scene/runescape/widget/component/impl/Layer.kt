package stan.qodat.scene.runescape.widget.component.impl

import stan.qodat.scene.runescape.widget.component.Component
import stan.qodat.scene.runescape.widget.component.LayerRef
import stan.qodat.scene.runescape.widget.component.Pos
import stan.qodat.scene.runescape.widget.component.Size

class Layer(
    override var name: String? = null,
    override var x: Int = 0,
    override var y: Int = 0,
    override var width: Int = 0,
    override var height: Int = 0,
    override var hSize: Size = Size.abs,
    override var vSize: Size = Size.abs,
    override var hPos: Pos = Pos.abs_left,
    override var vPos: Pos = Pos.abs_top
) : Component<LayerRef>() {

    val children = mutableListOf<Component<*>>()

    var scrollX : Int = 0
    var scrollY : Int = 0
    var scrollHeight: Int = 0

    fun layer(name: String? = null, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0, hSize: Size = Size.abs, vSize: Size = Size.abs, hPos: Pos = Pos.abs_left, vPos: Pos = Pos.abs_top, init: Layer.() -> Unit = {}): Layer =
        Layer(name, x, y, width, height, hSize, vSize, hPos, vPos).apply(init).apply(children::add)

    fun rectangle(name: String? = null, x : Int = 0, y : Int = 0, width : Int = 0, height : Int = 0, hSize : Size = Size.abs, vSize : Size = Size.abs, hPos: Pos = Pos.abs_left, vPos: Pos = Pos.abs_top, init : Rectangle.() -> Unit = {}) : Rectangle =
        Rectangle(name, x, y, width, height, hSize, vSize, hPos, vPos).apply(init).apply(children::add)

    fun graphic(name: String? = null, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0, hSize: Size = Size.abs, vSize: Size = Size.abs, hPos: Pos = Pos.abs_left, vPos: Pos = Pos.abs_top, init: Graphic.() -> Unit = {}): Graphic =
        Graphic(name, x, y, width, height, hSize, vSize, hPos, vPos).apply(init).apply(children::add)

    fun text(name: String? = null, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0, hSize: Size = Size.abs, vSize: Size = Size.abs, hPos: Pos = Pos.abs_left, vPos: Pos = Pos.abs_top, init: Text.() -> Unit = {}): Text =
        Text(name, x, y, width, height, hSize, vSize, hPos, vPos).apply(init).apply(children::add)

    override fun toReference(name: String) =
        LayerRef(name, this)
}
