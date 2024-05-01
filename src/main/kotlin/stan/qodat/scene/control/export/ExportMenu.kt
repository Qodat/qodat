package stan.qodat.scene.control.export

import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import stan.qodat.javafx.onChange
import stan.qodat.scene.control.export.wavefront.WaveFrontFormat
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

/**
 * Represents a [Menu] that can be used to export [exportable objects][T] to different file formats.
 *
 * TODO: prevent unnecessary invocations of [updateMenuItems]
 */
class ExportMenu<T : Exportable> : Menu("Export") {

    private val exportableProperty = SimpleObjectProperty<T>().apply {
        onInvalidation {
            updateMenuItems()
        }
    }

    private val animationProperty = SimpleObjectProperty<Animation>().apply {
        onInvalidation {
            updateMenuItems()
        }
    }

    private val animationFramesProperty = SimpleObjectProperty<List<AnimationFrame>>().apply {
        onInvalidation {
            updateMenuItems()
        }
    }

    fun setExportable(exportable: T) {
        exportableProperty.set(exportable)
    }

    fun setAnimation(animation: AnimationLegacy) {
        animationProperty.set(animation)
    }

    fun bindExportable(exportable: ReadOnlyObjectProperty<T>) {
        exportableProperty.unbind()
        exportableProperty.setAndBind(exportable)
    }

    fun bindAnimation(animation: Property<Animation>) {
        animationProperty.unbind()
        animationProperty.setAndBind(animation)
    }

    fun bindFrameList(frameList: ListView<AnimationFrame>) {
        frameList.selectionModel.selectedItems.onChange {
            animationFramesProperty.set(frameList.selectionModel.selectedItems.toList())
        }
    }

    private fun updateMenuItems() {

        val exportable = exportableProperty.get()?:return
        val animation = animationProperty.get()
        var animationFrames = animationFramesProperty.get()

        items.clear()

        items.add(Menu("wavefront (.obj/.mtl)").apply {
            styleClass += "wave-front-format-export-menu"
            items.add(Menu("single").apply {
                items.add(
                    ExportMenuItem(
                        context = Triple(exportable, null, null),
                        format = WaveFrontFormat.Single(),
                        prefixMenuItemName = "still"
                    )
                )
                items.add(Menu("animated").apply {
                    isDisable = animationFrames == null || animationFrames.isEmpty()
                    if (!isDisable) {
                        items.addAll(animationFrames.map { animationFrame ->
                            ExportMenuItem(
                                context = Triple(exportable, animation, animationFrame),
                                format = WaveFrontFormat.Single(),
                                prefixMenuItemName = "frame ${animationFrame.getName()} "
                            )
                        })
                    }
                })
            })

            var prefixName = "sequence"
            var disable = false
            if (animationFrames == null) {
                if (animation != null) {
                    prefixName += " (all frames)"
                    prefixName += " from animation ${animation.getName()}"
                    animationFrames = animation.getFrameList()
                } else {
                    prefixName += " (no animation selected)"
                    disable = true
                }
            } else {
                prefixName += " (${animationFrames.size} frames)"
                if (animation != null)
                    prefixName += " from animation ${animation.getName()}"
            }
            items.add(ExportMenuItem(
                context = mapOf(exportable to (animation to animationFrames)),
                format = WaveFrontFormat.Sequence(),
                prefixMenuItemName = prefixName
            ).apply {
                isDisable = disable
            })
        })
    }
}
