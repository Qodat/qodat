package stan.qodat.cache.impl.displee

import com.displee.cache.CacheLibrary
import jagex.Buffer
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226

fun main() {
    val cache = CacheLibrary("/Users/stan/.qodat/downloads/2025-08-27-rev232/cache")

    val animsIndex = cache.getIndex(IndexType.ANIMATIONS)
    val seqArchive = cache.getIndex(IndexType.CONFIGS).archive(ConfigType.SEQUENCE.id)!!
    val rev229 = cache.getIndex(IndexType.MODELS).revision >= 969
    val keyframesIndex = cache.index(22)

    val loader = SequenceLoader226()
    loader.configureForRevision(seqArchive.revision)
    for (seqFile in seqArchive.files()) {
        val seqData = seqFile.data ?: error("Sequence ${seqFile.id} data null")
        val seqDef = loader.load(seqFile.id, seqData)
        val frameMapArchiveIds = mutableSetOf<Int>()
        if (seqDef.animMayaId == -1) {
            val frameIds = seqDef.frameIDs?:continue
            for (frameId in frameIds) {
                val archiveId = frameId shr 16
                val fileId = frameId and 65535
                val archive = animsIndex.archive(archiveId) ?: error("Animation archive null for $archiveId file $fileId")
                val file = archive.file(fileId) ?: error("Animation file null for $archiveId file $fileId")
                val data = file.data?:error("Animation file data null for $archiveId file $fileId")
                if (data.isEmpty()) error("Animation file data empty for $archiveId file $fileId")
                val buffer = Buffer(data)
                val frameMapArchiveId = buffer.readUnsignedShort()
                frameMapArchiveIds += frameMapArchiveId
            }
//            println("DispleeMain: found frame map archive ids ${frameMapArchiveIds.size} in sequence ${seqFile.id}")
        } else {
            val index = if (rev229) keyframesIndex else animsIndex
            val archive = index.archive(seqDef.animMayaId shr 16) ?: error("Animation archive null for ${seqDef.animMayaId}")
            val file = archive.file(seqDef.animMayaId and 65535) ?: error("Animation file null for ${seqDef.animMayaId}")
            val data = file.data?:error("Animation file data null for ${seqDef.animMayaId}")
            if (data.isEmpty()) error("Animation file data empty for ${seqDef.animMayaId}")
            val buffer = Buffer(data)
            val version = buffer.readUnsignedByte()
            val frameMapArchiveId = buffer.readUnsignedShort()
            frameMapArchiveIds += frameMapArchiveId
            println("DispleeMain: found frame map archive id $frameMapArchiveId in sequence ${seqFile.id}")
        }
    }
}