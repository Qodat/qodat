package stan.qodat.scene.control.export.image

import qodat.cache.definition.SpriteDefinition
import stan.qodat.Properties
import stan.qodat.scene.runescape.ui.Sprite
import java.nio.file.Files
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpriteExportFormatTest {

    @Test
    fun pngIsTheOnlyRegisteredFormat() {
        assertEquals("PNG", SpriteExportFormat.PNG.formatName)
        assertEquals(listOf("png"), SpriteExportFormat.PNG.extensions)
        assertEquals(listOf(SpriteExportFormat.PNG), SpriteExportFormat.all)
        assertEquals(SpriteExportFormat.PNG, SpriteExportFormat.all.single())
    }

    @Test
    fun defaultDestinationIsExportsSprites() {
        assertEquals(
            Properties.defaultExportsPath.get().resolve("sprites"),
            SpriteExportFormat.PNG.defaultSaveDestinationProperty.get()
        )
        assertEquals(Properties.lastSpriteExportPath, SpriteExportFormat.PNG.lastSaveDestinationProperty)
    }

    @Test
    fun spriteFileNameUsesIdFrameAndFirstExtension() {
        val sprite = Sprite(FakeSpriteDefinition(id = 44, frame = 0))
        assertEquals("44[0]", sprite.getName())
        assertEquals("44[0].png", sprite.getName() + "." + SpriteExportFormat.PNG.extensions.first())

        val framed = Sprite(FakeSpriteDefinition(id = 7, frame = 3))
        assertEquals("7[3].png", framed.getName() + "." + SpriteExportFormat.PNG.extensions.first())
    }

    @Test
    fun directoryDestinationAppendsSpriteFileName() {
        val dir = Files.createTempDirectory("sprite-export")
        val file = Files.createTempFile("sprite-export-file", ".png")
        try {
            assertTrue(dir.isDirectory())
            assertFalse(file.isDirectory())
            val spriteName = Sprite(FakeSpriteDefinition(99, 1)).getName()
            val extension = SpriteExportFormat.PNG.extensions.first()
            val fromDirectory = if (dir.isDirectory()) dir.resolve("$spriteName.$extension") else dir
            val fromFile = if (file.isDirectory()) file.resolve("$spriteName.$extension") else file
            assertEquals(dir.resolve("99[1].png"), fromDirectory)
            assertEquals(file, fromFile)
        } finally {
            Files.deleteIfExists(file)
            dir.toFile().deleteRecursively()
        }
    }

    private class FakeSpriteDefinition(
        override val id: Int,
        override val frame: Int,
    ) : SpriteDefinition {
        override val offsetX = 0
        override val offsetY = 0
        override val width = 1
        override val height = 1
        override val pixels = intArrayOf(0)
        override val maxWidth = 1
        override val maxHeight = 1
        override var pixelIdx = byteArrayOf()
        override var palette = intArrayOf()
    }
}
