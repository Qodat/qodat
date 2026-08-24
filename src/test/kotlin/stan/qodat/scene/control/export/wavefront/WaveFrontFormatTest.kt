package stan.qodat.scene.control.export.wavefront

import javafx.beans.property.SimpleObjectProperty
import stan.qodat.Properties
import stan.qodat.scene.control.export.Exportable
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.util.Searchable
import stan.qodat.util.formatName
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WaveFrontFormatTest {

    @Test
    fun formatNameStripsLeadingColorTags() {
        assertEquals("Goblin", named("<col=ff0000>Goblin</col>").formatName())
        assertEquals("Guard", named("<col=00ff00>Guard").formatName())
        assertEquals("", named("<col=ff").formatName())
        assertEquals("plain name", named("plain name").formatName())
        assertEquals("has <col=x>inside", named("has <col=x>inside").formatName())
        assertEquals("  padded  ", named("  padded  ").formatName())
    }

    @Test
    fun singleFileNameUsesFormatNameWithoutReplacingSpaces() {
        val format = WaveFrontFormat.Single(SimpleObjectProperty<Path?>(null))
        val context = Triple<Searchable, Animation?, AnimationFrame?>(
            named("<col=ffffff>Iron platebody"),
            null,
            null
        )
        assertEquals("Iron platebody", format.getFileName(context))
        assertEquals("Export Iron platebody as WaveFront file", format.getFileChooserTitle(context))
    }

    @Test
    fun modelExportFileNameReplacesSpacesTheSameWayAsExportTo() {
        val fileName = named("abyssal demon").formatName().replace(" ", "_")
        assertEquals("abyssal_demon", fileName)
        assertEquals("a__b", named("a  b").formatName().replace(" ", "_"))
    }

    @Test
    fun sequenceTitleUsesMapSize() {
        val format = WaveFrontFormat.Sequence<Exportable>(SimpleObjectProperty<Path?>(null))
        assertEquals(
            "Export 0 models as a WaveFront sequence",
            format.getFileChooserTitle(emptyMap())
        )
        val none: Animation? = null
        val one: Map<Exportable, Pair<Animation?, List<AnimationFrame>>> =
            mapOf(namedExportable("a") to (none to emptyList()))
        assertEquals("Export 1 models as a WaveFront sequence", format.getFileChooserTitle(one))
        val two = one + (namedExportable("b") to (none to emptyList()))
        assertEquals("Export 2 models as a WaveFront sequence", format.getFileChooserTitle(two))
        assertNull(format.getFileName(one))
    }

    @Test
    fun defaultDestinationIsExportsWavefront() {
        val format = WaveFrontFormat.Single(SimpleObjectProperty<Path?>(null))
        assertEquals(
            Properties.defaultExportsPath.get().resolve("wavefront"),
            format.defaultSaveDestinationProperty.get()
        )
    }

    @Test
    fun initialDirectoryPrefersLastSavePath() {
        val last = SimpleObjectProperty<Path?>(null)
        val format = WaveFrontFormat.Single(last)
        assertEquals(format.defaultSaveDestinationProperty.get()?.toFile(), format.getFileChooserInitialDirectory(dummyContext()))

        val previous = Path.of("/tmp", "qodat-wavefront-last")
        last.set(previous)
        assertEquals(previous.toFile(), format.getFileChooserInitialDirectory(dummyContext()))
    }

    @Test
    fun sequenceDirectoryUsesFormattedNameAndOptionalAnimation() {
        val root = Path.of("/exports")
        val entityDir = root.resolve(named("<col=ff0000>King Black Dragon").formatName())
        assertEquals(Path.of("/exports", "King Black Dragon"), entityDir)
        assertEquals(
            Path.of("/exports", "King Black Dragon", "animation", "walk"),
            entityDir.resolve("animation/walk")
        )
    }

    private fun dummyContext(): Triple<Searchable, Animation?, AnimationFrame?> =
        Triple(named("x"), null, null)

    private fun named(name: String) = object : Searchable {
        override fun getName() = name
    }

    private fun namedExportable(name: String) = object : Exportable {
        override fun getName() = name
    }
}
