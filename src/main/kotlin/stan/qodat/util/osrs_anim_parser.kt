package stan.qodat.util

import com.google.gson.GsonBuilder
import jagex.Buffer
import javafx.application.Platform
import javafx.concurrent.Task
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.NpcManager
import net.runelite.cache.ObjectManager
import net.runelite.cache.definitions.loaders.SequenceLoader
import net.runelite.cache.fs.Archive
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
    npcManager: NpcManager,
) = object : LoadAnimationTask(store) {
    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val total = npcManager.npcs.size
        val counter = AtomicInteger(0)
        npcManager.npcs.parallelStream().forEach { npc ->
            val animationRef = intArrayOf(
                npc.walkingAnimation,
                npc.standingAnimation,
                npc.idleRotateLeftAnimation ,
                npc.idleRotateRightAnimation ,
                npc.rotateLeftAnimation ,
                npc.rotateRightAnimation,
                npc.rotate180Animation
            ).filter { it > 0 }
            try {

                if (animationRef.isNotEmpty()) {

                    val referenceFrames = animationRef.flatMap {
                        requireNotNull(skeletonIdsByAnimationId[it]) {
                            "Animation $it was null for npc ${npc.name}"
                        }
                    }.toSet()

                    val matches = skeletonIdsByAnimationId.filter { entry ->
                        entry.value.any {
                            referenceFrames.any { reference ->
                                reference == it
                            }
                        }
                    }
                    try {
                        val file = Properties.osrsCachePath.get().resolve("npc_anims/${npc.id}.json").toFile()
                        val writer = FileWriter(file)
                        gson.toJson(matches.keys.toIntArray(), writer)
                        writer.flush()
                        writer.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Platform.runLater {
                        val i = counter.incrementAndGet()
                        val progress = (100.0 * i.toFloat().div(total))
                        updateProgress(progress, 100.0)
                        updateMessage("Parsed npc ($i / $total})")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun createObjectAnimsJsonDir(
    store: Store,
    objectManager: ObjectManager,
) = object : LoadAnimationTask(store) {
    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val total = objectManager.objects.size
        val counter = AtomicInteger(0)
        objectManager.objects.parallelStream().forEach { objectDefinition ->
            val animationRef = objectDefinition.animationID

            if (animationRef > 0) {
                val referenceFrames = skeletonIdsByAnimationId[animationRef]!!
                val matches = skeletonIdsByAnimationId.filter { entry ->
                    entry.value.any { referenceFrames.any { reference -> reference == it } }
                }
                try {
                    val file =
                        Properties.osrsCachePath.get().resolve("object_anims/${objectDefinition.id}.json").toFile()
                    val writer = FileWriter(file)
                    gson.toJson(matches.keys.toIntArray(), writer)
                    writer.flush()
                    writer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Platform.runLater {
                    val i = counter.incrementAndGet()
                    val progress = (100.0 * i.toFloat().div(total))
                    updateProgress(progress, 100.0)
                    updateMessage("Parsed object ($i / $total})")
                }
            }
        }
    }
}



abstract class LoadAnimationTask(
    private val store: Store,
) : Task<Void?>() {

    private val map = ConcurrentHashMap<Int, ArchiveFiles>()
    private val storage = store.storage
    private val index by lazy { store.getIndex(IndexType.CONFIGS) }
    private val seqArchive by lazy { index.getArchive(ConfigType.SEQUENCE.id) }
    private val archiveData by lazy { storage.loadArchive(seqArchive) }
    private val files by lazy { seqArchive.getFiles(archiveData) }
    private val animIndex by lazy { store.getIndex(IndexType.ANIMATIONS) }
    private val animationFiles by lazy { files.files }

    override fun call(): Void? {
        val skeletonIdsByAnimationId = associateAnimationBySkeletonIds()
        updateMessage("Loaded all animation mappings!")
        matchAnimationsToSkeletons(skeletonIdsByAnimationId)
        return null
    }

    abstract fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>)

    private fun associateAnimationBySkeletonIds() = animationFiles.parallelStream().map { file ->
        val loader = SequenceLoader()
        loader.configureForRevision(seqArchive.revision)
        val anim = loader.load(file.fileId, file.contents)
        Platform.runLater {
            val progress = (100.0 * anim.id.toFloat().div(animationFiles.size))
            updateProgress(progress, 100.0)
            updateMessage("Parsed animation (${anim.id + 1} / ${animationFiles.size}})")
        }
        val frameGroupIds: Set<Int> = if (anim.animMayaID >= 0) {
            val animArchive: Archive = animIndex.getArchive(anim.animMayaID shr 16 and '\uffff'.code)
            val animData = store.storage.loadArchive(animArchive)
            val animFiles = animArchive.getFiles(animData)
            val animFile = animFiles.findFile(anim.animMayaID and '\uffff'.code)
            val data = animFile.contents
            val buffer = Buffer(data)
            val version = buffer.readUnsignedByte()
            val frameGroupId = buffer.readUnsignedShort()
            setOf(frameGroupId)
        } else anim.frameIDs?.map {
            val frameArchiveId = it shr 16
            val frameArchiveFileId = it and 65535

            val frameArchive = requireNotNull(animIndex.getArchive(frameArchiveId))
            { "Frame group null for $frameArchiveId file $frameArchiveFileId" }
            val frameArchiveFiles = map.getOrPut(frameArchiveId) {
                frameArchive.getFiles(storage.loadArchive(frameArchive))!!
            }
            val frameFile = frameArchiveFiles.findFile(frameArchiveFileId)!!
            val frameContents = frameFile.contents

            val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
            frameMapArchiveId
        }?.toSet() ?: emptySet()
        (anim.id to frameGroupIds)
    }.collect(Collectors.toList()).toMap()
}
