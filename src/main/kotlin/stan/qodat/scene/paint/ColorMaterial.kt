package stan.qodat.scene.paint

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.scene.layout.HBox
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import stan.qodat.util.DEFAULT
import stan.qodat.util.ModelUtil

data class ColorMaterial(private val encodedColor: Short, private val alpha: Byte?) :
    Material {

    private val color by lazy {
        ModelUtil.hsbToColor(encodedColor, alpha)
    }

    override val fxMaterial by lazy {
        PhongMaterial(color)
    }

    private val viewNode by lazy {
        HBox().apply {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            children += Text("Color\t").apply {
                fill = DEFAULT
            }
            children += Rectangle().apply {
                width = 15.0
                height = 15.0
                fill = color
            }
            children += Label(color.toString()).apply {
                contextMenu = ContextMenu(
                    MenuItem("Copy HEX Color Value").apply {
                        setOnAction {
                            Clipboard.getSystemClipboard().setContent(
                                mapOf(DataFormat.PLAIN_TEXT to color.toString())
                            )
                        }
                    },
                    MenuItem("Copy RS Color Value").apply {
                        setOnAction {
                            Clipboard.getSystemClipboard().setContent(
                                mapOf(DataFormat.PLAIN_TEXT to encodedColor.toString())
                            )
                        }
                    }
                )
            }
            if (alpha != null) {
                val alphaValue = alpha.toUByte().toString()
                children += Text("Alpha").apply {
                    fill = DEFAULT
                }
                children += Label(alphaValue).apply {
                    contextMenu = ContextMenu(
                        MenuItem("Copy RS Alpha Value").apply {
                            setOnAction {
                                Clipboard.getSystemClipboard().setContent(
                                    mapOf(DataFormat.PLAIN_TEXT to alphaValue)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun getViewNode(): Node = viewNode
}