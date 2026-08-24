package stan.qodat.scene.runescape.ui

import com.displee.cache.CacheLibrary
import stan.qodat.cache.impl.displee.types.InterfaceManager
import stan.qodat.cache.impl.displee.types.SpriteManager
import stan.qodat.cache.impl.oldschool.definition.RuneliteSpriteDefinition
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.test.Test

/**
 * Writes software-blit PNGs of well-known OSRS groups for visual comparison.
 * Skips when the local rev239 cache is not present.
 */
class InterfaceOsrsCompareDump {

    @Test
    fun dumpKnownInterfaces() {
        val cacheDir = Path.of(System.getProperty("user.home"), ".qodat", "downloads", "2026-06-30-rev239-2", "cache")
        if (!cacheDir.resolve("main_file_cache.dat2").exists())
            return
        val out = Path.of("/tmp/qodat-iface-compare")
        Files.createDirectories(out)
        val library = CacheLibrary(cacheDir.toAbsolutePath().toString())
        val interfaces = InterfaceManager(library)
        val sprites = SpriteManager(library)
        val notes = StringBuilder()
        for ((id, label) in GROUPS) {
            val defs = InterfaceManager.mapDispleeInterface(interfaces.getInterfaceGroup(id)).toList()
            if (defs.isEmpty()) {
                notes.appendLine("$id $label: missing")
                continue
            }
            val lookup: (Int) -> RuneliteSpriteDefinition? = { spriteId ->
                sprites.findSprite(spriteId, 0)?.let(::RuneliteSpriteDefinition)
            }
            val image = InterfaceRaster.render(defs, lookup)
            val file = out.resolve("$id-$label.png").toFile()
            ImageIO.write(image, "png", file)
            ImageIO.write(
                InterfaceRaster.render(defs, lookup, includeHidden = true),
                "png",
                out.resolve("$id-$label-hidden.png").toFile(),
            )
            val hidden = defs.count { it.isHidden }
            val onLoad = defs.count { !it.onLoadListener.isNullOrEmpty() }
            val scripts1 = defs.count { !it.clientScripts.isNullOrEmpty() }
            val typed = defs.groupingBy { it.type }.eachCount().toSortedMap()
            val spritesSet = defs.count { it.type == 5 && it.spriteId >= 0 && !it.isHidden }
            val spritesHidden = defs.count { it.type == 5 && it.spriteId >= 0 && it.isHidden }
            val spritesUnset = defs.count { it.type == 5 && it.spriteId < 0 }
            notes.appendLine(
                "$id $label: widgets=${defs.size} hidden=$hidden onLoad=$onLoad cs1=$scripts1 if3=${defs.count { it.isIf3 }} types=$typed visibleSprites=$spritesSet hiddenSprites=$spritesHidden unsetSprites=$spritesUnset -> ${file.path}"
            )
            defs.filter { !it.onLoadListener.isNullOrEmpty() }.take(6).forEach { def ->
                notes.appendLine("  onLoad child=${def.id and 0xffff} hidden=${def.isHidden} type=${def.type} sprite=${def.spriteId} args=${def.onLoadListener!!.toList()}")
            }
        }
        Files.writeString(out.resolve("stats.txt"), notes.toString())
        println(notes)
        library.close()
    }

    companion object {
        private val GROUPS = listOf(
            12 to "bank",
            15 to "bank-side",
            149 to "inventory",
            162 to "chatbox",
            218 to "magic",
            541 to "prayer",
            593 to "combat",
            161 to "toplevel",
        )
    }
}
