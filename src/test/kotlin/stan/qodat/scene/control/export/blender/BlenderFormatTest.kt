package stan.qodat.scene.control.export.blender

import javafx.beans.property.SimpleObjectProperty
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.util.Searchable
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class BlenderFormatTest {

    @Test
    fun directoryDestinationAppendsGlbFileName() {
        val format = BlenderFormat(SimpleObjectProperty(null))
        val context = Triple<Searchable, Animation?, AnimationFrame?>(named("abyssal demon"), null, null)
        val dir = Path.of("/exports", "blender")
        assertEquals(Path.of("/exports", "blender", "abyssal_demon.glb"), format.resolveSaveFile(context, dir))
    }

    @Test
    fun glbDestinationIsUsedAsIs() {
        val format = BlenderFormat(SimpleObjectProperty(null))
        val context = Triple<Searchable, Animation?, AnimationFrame?>(named("guard"), null, null)
        val file = Path.of("/tmp", "custom.glb")
        assertEquals(file, format.resolveSaveFile(context, file))
    }

    private fun named(name: String) = object : Searchable {
        override fun getName() = name
    }
}
