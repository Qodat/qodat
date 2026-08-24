package stan.qodat.scene.runescape.ui

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import qodat.cache.Cache
import qodat.cache.definition.SpriteDefinition
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.export.ExportMenuItem
import stan.qodat.scene.control.export.image.SpriteExportFormat
import stan.qodat.scene.presentation.PlanarSceneHandle
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.util.Searchable
import tornadofx.contextmenu
import java.nio.IntBuffer

class Sprite(
    definition: SpriteDefinition,
    archiveFrames: List<SpriteDefinition> = listOf(definition),
    private val cache: Cache? = null,
) : SceneNodeProvider, ViewNodeProvider, Searchable {

    private val listId = definition.id
    private val listFrame = definition.frame
    private var resolvedDef = definition
    private var resolvedFrames = archiveFrames
    private var resolved = cache == null

    val nameProperty = SimpleStringProperty("$listId[$listFrame]")
    val archiveFrames: List<SpriteDefinition>
        get() {
            ensureResolved()
            return SpriteSceneBuilder.archiveFrames(resolvedDef, resolvedFrames)
        }

    val definition: SpriteDefinition
        get() {
            ensureResolved()
            return resolvedDef
        }

    val image: Image by lazy { imageOf(this.definition) }

    val preview: ImageView by lazy {
        ImageView(image).apply {
            isPreserveRatio = true
            fitWidth = 24.0
            fitHeight = 24.0
        }
    }

    private var sceneBuilder: SpriteSceneBuilder? = null
    private val planar = PlanarSceneHandle { exploded, animate ->
        sceneBuilder?.applyExploded(exploded, animate)
    }

    private val sceneGroup: Group by lazy {
        val builder = SpriteSceneBuilder(this.definition, this.archiveFrames)
        sceneBuilder = builder
        val content = builder.build()
        planar.bind()
        Group(content)
    }

    val viewNode: HBox by lazy {
        LabeledHBox(nameProperty, labelPrefix = "sprite").apply {
            contextmenu {
                styleClass += "wave-front-format-export-menu"
                for (exportFormat in SpriteExportFormat.all)
                    items.add(ExportMenuItem(this@Sprite, exportFormat, "Export as ${exportFormat.formatName}"))
            }
        }
    }

    override fun getSceneNode(): Node {
        val node = sceneGroup
        planar.bind()
        return node
    }

    override fun getViewNode(): Node = viewNode

    override fun removeSceneNodeReference() {
        planar.unbind()
    }

    override fun getName(): String = nameProperty.get()

    private fun ensureResolved() {
        if (resolved) return
        resolved = true
        val frames = cache?.getSpriteArchive(listId) ?: return
        if (frames.isEmpty()) return
        resolvedFrames = frames.toList()
        resolvedDef = frames.firstOrNull { it.frame == listFrame } ?: frames.first()
    }

    companion object {
        fun imageOf(definition: SpriteDefinition): Image {
            val width = definition.width.coerceAtLeast(1)
            val height = definition.height.coerceAtLeast(1)
            val image = WritableImage(width, height)
            if (definition.width > 0 && definition.height > 0 && definition.pixels.size >= width * height) {
                val pixels = IntBuffer.wrap(definition.pixels)
                image.pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, width)
            }
            return image
        }
    }
}
