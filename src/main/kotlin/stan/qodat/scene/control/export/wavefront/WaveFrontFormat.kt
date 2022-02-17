package stan.qodat.scene.control.export.wavefront

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.stage.DirectoryChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.control.export.ExportFormat
import stan.qodat.scene.control.export.Exportable
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.task.BackgroundTasks
import stan.qodat.task.export.impl.ExportWaveFrontTask
import stan.qodat.util.Searchable
import java.io.File
import java.nio.file.Path

sealed class WaveFrontFormat<C> : ExportFormat<C> {

    override val defaultSaveDestinationProperty: ObjectBinding<Path> =
        Bindings.createObjectBinding(
            { Properties.defaultExportsPath.get().resolve("wavefront") },
            Properties.defaultExportsPath
        )

    abstract fun exportTo(context: C, path: Path, addProgressIndicator: Boolean)

    abstract fun getFileChooserTitle(context: C): String

    open fun getFileChooserInitialFileName(context: C): String? = null

    open fun getFileChooserInitialDirectory(context: C): File? =
        (lastSaveDestinationProperty.value?:defaultSaveDestinationProperty.get())?.toFile()

    open fun resolveFile(context: C, directory: File): File = directory

    override fun chooseSaveDestination(context: C): File? {
        val initialDirectory = getFileChooserInitialDirectory(context)
        if (initialDirectory != null) {
            if (!initialDirectory.exists()) {
                initialDirectory.mkdirs()
            }
        }
        val fileChooser = DirectoryChooser().apply {
            this.title = getFileChooserTitle(context)
            this.initialDirectory = initialDirectory
        }
        return fileChooser.showDialog(Qodat.stage)?.let {
            lastSaveDestinationProperty.set(it.toPath())
            it
        }
    }

    final override fun export(context: C, destination: Path) {
        try {
            exportTo(context, destination, true)
        } catch (e: Exception) {
            Qodat.logException("Failed to execute export task", e)
        }
    }


    class Single(
        override val lastSaveDestinationProperty: ObjectProperty<Path?> =
            Properties.lastWaveFrontSingleExportPath
    ) : WaveFrontFormat<Triple<Searchable, Animation?, AnimationFrame?>>() {

        override fun getFileName(context: Triple<Searchable, Animation?, AnimationFrame?>): String =
            context.first.getName()

        override fun getFileChooserTitle(context: Triple<Searchable, Animation?, AnimationFrame?>) =
            "Export ${context.first.getName()} as WaveFront file"

        override fun getFileChooserInitialFileName(context: Triple<Searchable, Animation?, AnimationFrame?>): String {
            val (searchable, animation, animationFrame) = context
            var base = searchable.getName()
            if (animationFrame != null) {
                if (animation != null) {
                    base += "_${animation.getName()}_${animationFrame.getName()}"
                }
            }
            return base
        }

        override fun resolveFile(context: Triple<Searchable, Animation?, AnimationFrame?>, directory: File): File {
            val (searchable, animation, animationFrame) = context
            var base = directory.resolve("${searchable.getName()}/still")
            if (animationFrame != null) {
                if (animation != null) {
                    base = base.resolve("_A_${animation.getName()}_F_${animationFrame.getName()}")
                }
            }
            return base
        }

        override fun exportTo(
            context: Triple<Searchable, Animation?, AnimationFrame?>,
            path: Path,
            addProgressIndicator: Boolean
        ) {
            val (exportable, _, animationFrame) = context
            val task = when (exportable) {
                is Entity<*> -> ExportWaveFrontTask.Single(
                    saveDir = path,
                    entity = exportable,
                    animationFrame = animationFrame
                )
                is Model -> ExportWaveFrontTask.Single(
                    saveDir = path,
                    model = exportable,
                    animationFrame = animationFrame,
                    fileName = exportable.getName().replace(" ", "_")
                )
                else -> throw Exception("Could not export ${exportable.getName()} of type ${exportable::class.java}.")
            }
            task.setOnFailed {
                Qodat.logException("Failed to execute task ${task.title}", task.exception)
            }
            BackgroundTasks.submit(addProgressIndicator, task)
        }
    }

    class Sequence<T : Exportable>(
        override val lastSaveDestinationProperty: ObjectProperty<Path?> =
            Properties.lastWaveFrontSequenceExportPath
    ) : WaveFrontFormat<Map<T, Pair<Animation?, List<AnimationFrame>>>>() {

        override fun getFileChooserTitle(context: Map<T, Pair<Animation?, List<AnimationFrame>>>) =
            "Export ${context.size} models as a WaveFront sequence"

        override fun exportTo(
            context: Map<T, Pair<Animation?, List<AnimationFrame>>>,
            path: Path,
            addProgressIndicator: Boolean
        ) {
            BackgroundTasks.submit(addProgressIndicator) {
                context.forEach { (exportable, animationFramesPair) ->

                    val (animation, animationFrames) = animationFramesPair
                    var directory = path.resolve(exportable.getName())

                    if (animation != null)
                        directory = directory.resolve("animation/${animation.getName()}")

                    directory.toFile().apply {
                        if (!exists()) {
                            mkdirs()
                        }
                    }

                    val task = when (exportable) {
                        is Entity<*> -> ExportWaveFrontTask.Sequence(
                            saveDir = directory,
                            exportable,
                            animationFrames
                        )
                        is Model -> ExportWaveFrontTask.Sequence(saveDir = directory, exportable, animationFrames)
                        else -> throw Exception("Could not export ${exportable.getName()} of type ${exportable::class.java}.")
                    }

                    task.setOnFailed {
                        Qodat.logException("Failed to execute task ${task.title}", task.exception)
                    }

                    BackgroundTasks.submit(addProgressIndicator, task)
                }
            }
        }
    }
}