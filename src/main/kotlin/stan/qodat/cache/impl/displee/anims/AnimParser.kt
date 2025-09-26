package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import com.google.gson.GsonBuilder
import jagex.Buffer
import javafx.application.Platform
import javafx.concurrent.Task
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.runelite.cache.IndexType
import stan.qodat.cache.impl.displee.getIndex
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226
import java.util.stream.Collectors


abstract class AnimParser(
    private val cacheLibrary: CacheLibrary
) : Task<Void?>() {

    protected val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val globalMutex = Mutex(false)
    }

    private val animIndex by lazy {
        cacheLibrary.index(0)
    }
    private val seqArchive by lazy { cacheLibrary.index(2).archive(12)!! }
    private val animationFiles get() =
        seqArchive.files

    val loader by lazy {
        SequenceLoader226().apply { configureForRevision(seqArchive.revision) }
    }

    override fun call(): Void? {
        val skeletonIdsByAnimationId = associateAnimationBySkeletonIds()
        updateMessage("Loaded all animation mappings!")
        matchAnimationsToSkeletons(skeletonIdsByAnimationId)
        return null
    }

    abstract fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>)

    private fun associateAnimationBySkeletonIds(): Map<Int, Set<Int>> {
        val rev229 = cacheLibrary.getIndex(IndexType.MODELS).revision >= 969
        return animationFiles.values.parallelStream().map { file ->
            val anim = loader.load(file.id, file.data ?: error("Animation data null"))
            Platform.runLater {
                val progress = (100.0 * anim.id.toFloat().div(animationFiles.size))
                updateProgress(progress, 100.0)
                updateMessage("Parsed animation (${anim.id + 1} / ${animationFiles.size}})")
            }
            val frameGroupIds: Set<Int> = if (anim.animMayaId >= 0) {
                val animArchive = runBlocking {
                    globalMutex.withLock {
                        (if (rev229)
                            cacheLibrary.index(22)
                        else
                            animIndex).archive(anim.animMayaId shr 16)
                            ?: error("Animation archive null for ${anim.animMayaId}")
                    }
                }
                val animFile =
                    animArchive.file(anim.animMayaId and 65535) ?: error("Animation file null for ${anim.animMayaId}")
                val data = animFile.data
                val buffer = Buffer(data)
                val version = buffer.readUnsignedByte()
                val frameGroupId = buffer.readUnsignedShort()
                setOf(frameGroupId)
            } else anim.frameIDs?.map {
                val frameArchiveId = it shr 16
                val frameArchiveFileId = it and 65535
                val frameArchive = runBlocking {
                    globalMutex.withLock {
                        requireNotNull(animIndex.archive(frameArchiveId)) { "Frame group null for $frameArchiveId file $frameArchiveFileId" }
                    }
                }
                val frameFile = frameArchive.file(frameArchiveFileId)!!
                val frameContents = frameFile.data ?: error("Frame ${frameArchiveId}-${frameArchiveFileId} data null")
                if (frameContents.isEmpty()) error("Frame ${frameArchiveId}-${frameArchiveFileId} data empty")
                if (frameContents.size < 2) error("Frame ${frameArchiveId}-${frameArchiveFileId} data too small")
                val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                frameMapArchiveId
            }?.toSet() ?: emptySet()
            (anim.id.toInt() to frameGroupIds)
        }.collect(Collectors.toList()).toMap()
    }
}