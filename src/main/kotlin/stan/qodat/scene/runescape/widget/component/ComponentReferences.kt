package stan.qodat.scene.runescape.widget.component

import stan.qodat.scene.runescape.widget.Widget
import stan.qodat.scene.runescape.widget.component.impl.*

abstract class Ref<T> {
    abstract val name: String
    abstract val value: T
}

class WidgetRef(override val name: String, override val value: Widget) : Ref<Widget>()

class LayerRef(override val name: String, override val value: Layer) : Ref<Layer>()
class GraphicRef(override val name: String, override val value: Graphic) : Ref<Graphic>()
class RectangleRef(override val name: String, override val value: Rectangle) : Ref<Rectangle>()
class TextRef(override val name: String, override val value: Text) : Ref<Text>()
class ModelListRef(override val name: String, override val value: ModelList) : Ref<ModelList>()
class InventoryRef(override val name: String, override val value: Inventory) : Ref<Inventory>()
class ModelRef(override val name: String, override val value: Model) : Ref<Model>()
class LineRef(override val name: String, override val value: Line) : Ref<Line>()
