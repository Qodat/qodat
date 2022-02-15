package stan.qodat.scene.runescape.ui

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import qodat.cache.definition.SpriteDefinition
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.util.Searchable
import java.nio.IntBuffer

class Sprite(definition: SpriteDefinition) : SceneNodeProvider, ViewNodeProvider, Searchable {

    val nameProperty = SimpleStringProperty(definition.id.toString())

    val image : Image by lazy {
        val width = definition.width
        val height = definition.height
        val image = WritableImage(width, height)
        val pixels = IntBuffer.wrap(definition.pixels)
        image.pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, width)
        image
    }

    val sceneNode : ImageView by lazy { ImageView(image) }

    val viewNode : HBox by lazy { LabeledHBox(nameProperty) }

    override fun getSceneNode(): Node = sceneNode

    override fun getViewNode(): Node = viewNode

    override fun getName(): String  = nameProperty.get()
}