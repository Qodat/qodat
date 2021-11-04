package stan.qodat.util

import com.google.gson.GsonBuilder
import com.sun.javafx.application.PlatformImpl
import javafx.concurrent.Task
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.NpcManager
import net.runelite.cache.ObjectManager
import net.runelite.cache.definitions.loaders.SequenceLoader
import net.runelite.cache.fs.ArchiveFiles
import net.runelite.cache.fs.Store
import stan.qodat.Properties
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   04/09/2019
 * @version 1.0
 */

private val gson = GsonBuilder().setPrettyPrinting().create()

fun createNpcAnimsJsonDir(
    store: Store,
    npcManager: NpcManager
) = object : Task<Void?>() {
    override fun call(): Void? {
        val map = ConcurrentHashMap<Int, ArchiveFiles>()
        val storage = store.storage
        val index = store.getIndex(IndexType.CONFIGS)
        val seqArchive = index.getArchive(ConfigType.SEQUENCE.id)
        val archiveData = storage.loadArchive(seqArchive)
        val files = seqArchive.getFiles(archiveData)
        val frameIndex = store.getIndex(IndexType.FRAMES)
        val animationFiles = files.files
        val animations = animationFiles.parallelStream().map { file ->
            val loader = SequenceLoader()
            val anim = loader.load(file.fileId, file.contents)
            PlatformImpl.runLater {
                val progress = (100.0 * anim.id.toFloat().div(animationFiles.size))
                updateProgress(progress, 100.0)
                updateMessage("Parsed animation (${anim.id + 1} / ${animationFiles.size}})")
            }
            val frames: Set<Int> =  anim.frameIDs?.map {
                val hexString = Integer.toHexString(it)
                if (hexString.length < 6) {
                    println("Could not parse frame[$it] in anim[${anim.id}]")
                    null
                } else {
                    val frameArchiveId = decodeArchiveId(hexString)
                    val frameArchiveFileId = decodeArchiveFileId(hexString)

                    val frameArchive = frameIndex.getArchive(frameArchiveId)!!
                    val frameArchiveFiles = map.getOrPut(frameArchiveId) {
                        frameArchive.getFiles(storage.loadArchive(frameArchive))!!
                    }
                    val frameFile = frameArchiveFiles.findFile(frameArchiveFileId)!!
                    val frameContents = frameFile.contents

                    val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                    frameMapArchiveId
                }
            }?.filterNotNull()?.toSet()?: emptySet()
            (anim.id to frames)
        }.collect(Collectors.toList()).toMap()

        updateMessage("Loaded all animation mappings!")


        val total = npcManager.npcs.size
        val counter = AtomicInteger(0)
        npcManager.npcs.parallelStream().forEach { npc ->
            val animationRef = intArrayOf(
                npc.walkingAnimation,
                npc.standingAnimation,
                npc.rotateLeftAnimation,
                npc.rotateRightAnimation,
                npc.rotate90LeftAnimation,
                npc.rotate90RightAnimation,
                npc.rotate180Animation
            ).filter { it > 0 }

            if (animationRef.isNotEmpty()) {

                val referenceFrames = animationRef.flatMap { animations[it]!! }.toSet()

                val matches = animations.filter { entry ->
                    entry.value.any {
                        referenceFrames.any { reference ->
                            reference == it
                        }
                    }
                }
                var name = npc.name
                if (name.contains(">")) {
                    name = name.substringAfter('>').substringBeforeLast('<')
                }
                try {
                    val file = Properties.osrsCachePath.get().resolve("npc_anims/${name}.json").toFile()
                    val writer = FileWriter(file)
                    gson.toJson(matches.keys.toIntArray(), writer)
                    writer.flush()
                    writer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                PlatformImpl.runLater {
                    val i = counter.incrementAndGet()
                    val progress = (100.0 * i.toFloat().div(total))
                    updateProgress(progress, 100.0)
                    updateMessage("Parsed npc ($i / $total})")
                }
            }
        }
        return null
    }
}

fun createObjectAnimsJsonDir(
    store: Store,
    objectManager: ObjectManager
) = object : Task<Void?>() {
    override fun call(): Void? {
        val map = ConcurrentHashMap<Int, ArchiveFiles>()
        val storage = store.storage
        val index = store.getIndex(IndexType.CONFIGS)
        val seqArchive = index.getArchive(ConfigType.SEQUENCE.id)
        val archiveData = storage.loadArchive(seqArchive)
        val files = seqArchive.getFiles(archiveData)
        val frameIndex = store.getIndex(IndexType.FRAMES)
        val animationFiles = files.files
        val animations = animationFiles.parallelStream().map { file ->
            val loader = SequenceLoader()
            val anim = loader.load(file.fileId, file.contents)
            PlatformImpl.runLater {
                val progress = (100.0 * anim.id.toFloat().div(animationFiles.size))
                updateProgress(progress, 100.0)
                updateMessage("Parsed animation (${anim.id + 1} / ${animationFiles.size}})")
            }
            val frames: Set<Int> =  anim.frameIDs?.map {
                val hexString = Integer.toHexString(it)
                if (hexString.length < 6) {
                    println("Could not parse frame[$it] in anim[${anim.id}]")
                    null
                } else {
                    val frameArchiveId = decodeArchiveId(hexString)
                    val frameArchiveFileId = decodeArchiveFileId(hexString)

                    val frameArchive = frameIndex.getArchive(frameArchiveId)!!
                    val frameArchiveFiles = map.getOrPut(frameArchiveId) {
                        frameArchive.getFiles(storage.loadArchive(frameArchive))!!
                    }
                    val frameFile = frameArchiveFiles.findFile(frameArchiveFileId)!!
                    val frameContents = frameFile.contents

                    val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                    frameMapArchiveId
                }
            }?.filterNotNull()?.toSet()?: emptySet()
            (anim.id to frames)
        }.collect(Collectors.toList()).toMap()

        updateMessage("Loaded all animation mappings!")


        val total = objectManager.objects.size
        val counter = AtomicInteger(0)
        objectManager.objects.parallelStream().forEach { objectDefinition ->
            val animationRef = objectDefinition.animationID

            if (animationRef > 0) {

                val referenceFrames = animations[animationRef]!!

                val matches = animations.filter { entry ->
                    entry.value.any {
                        referenceFrames.any { reference ->
                            reference == it
                        }
                    }
                }
                var name = objectDefinition.name
                if (name.contains(">")) {
                    name = name.substringAfter('>').substringBeforeLast('<')
                }
                try {
                    val file = Properties.osrsCachePath.get().resolve("object_anims/${name}.json").toFile()
                    val writer = FileWriter(file)
                    gson.toJson(matches.keys.toIntArray(), writer)
                    writer.flush()
                    writer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                PlatformImpl.runLater {
                    val i = counter.incrementAndGet()
                    val progress = (100.0 * i.toFloat().div(total))
                    updateProgress(progress, 100.0)
                    updateMessage("Parsed object ($i / $total})")
                }
            }
        }
        return null
    }
}
private fun decodeArchiveId(hexString: String): Int {
    return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
}

private fun decodeArchiveFileId(hexString: String): Int {
    return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
}