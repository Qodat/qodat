package stan.qodat.scene.runescape.animation

import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.fs.ArchiveFiles
import net.runelite.cache.fs.Container
import net.runelite.cache.fs.FSFile
import net.runelite.cache.index.FileData
import qodat.cache.definition.AnimationSkeletonDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class AnimationExporter {

    fun encode(animation: Animation){
        val out = ByteArrayOutputStream()
        val os = DataOutputStream(out)

        val frames = animation.getFrameList()
        val firstFrame = frames.first()

        val group = firstFrame.definition!!.transformationGroup
        os.write(encodeFrameMap(group))

        val cache = OldschoolCacheRuneLite
        val storage = cache.store.storage

        val index = cache.store.getIndex(IndexType.CONFIGS)
        val archive = index.getArchive(ConfigType.SEQUENCE.id)

        val archiveData = storage.loadArchive(archive)
        val files = archive.getFiles(archiveData)

        val animationId = animation.idProperty.get()
        val file = files.findFile(animationId)!!

        val initialFileContent = file.contents

        val frameArchiveId = packFrames(animation)
        for ((frameIdx, frame) in animation.getFrameList().withIndex()) {
            val newId =  ((frameArchiveId and 0xFFFF) shl 16) or (frameIdx and 0xFFFF) // New frame id
            frame.idProperty.set(newId)
        }

        file.contents = encodeSequence(animation)

        val container = Container(archive.compression, -1)
        container.compress(files.saveContents(), null)
        val compressedData = container.data

        val decompressed = Container.decompress(compressedData, null)

        val decomData = decompressed.data

        println(initialFileContent.size)
        println(decomData.size)
        println()
//        storage.saveArchive(archive, compressedData)


//        storage.save(cache.store)
    }

    private fun packFrames(animation: Animation): Int {
        val store = OldschoolCacheRuneLite.store
        val frameIndex = store.getIndex(IndexType.FRAMES)

        val curArchives = animation.getFrameList().map {
            val hexString = Integer.toHexString(it.idProperty.get())
            val frameArchiveId = OldschoolCacheRuneLite.getFileId(hexString)
            val frameArchiveFileId = OldschoolCacheRuneLite.getFrameId(hexString)
            frameArchiveId to frameArchiveFileId
        }.groupBy { it.first }.map { entry ->
            val archiveId = entry.key
            val archive = frameIndex.getArchive(archiveId)
            val archiveData = OldschoolCacheRuneLite.store.storage.loadArchive(archive)
            val files = archive.getFiles(archiveData)
            archiveId to files.files.filter {
                entry.value.any { (_, fileId) ->
                    it.fileId == fileId
                }
            }
        }
        val archiveId = frameIndex.archives.maxByOrNull { it.archiveId }!!.archiveId + 1
        val archive = frameIndex.addArchive(archiveId)!!

        val archiveFiles = ArchiveFiles()

        archive.fileData = Array(animation.getFrameList().size) {
            FileData().apply {
                id = it
            }
        }

        for ((id, animationFrame) in animation.getFrameList().withIndex()) {
            val file = FSFile(id).apply {
                contents = encodeFrame(animationFrame)
            }
            archiveFiles.addFile(file)
        }

        val container = Container(archive.compression, -1)
        container.compress(archiveFiles.saveContents(), null)
        val compressedData = container.data

//        store.storage.saveArchive(archive, compressedData)
        return archiveId
    }

    fun encodeSequence(animation: Animation) : ByteArray {
        val out = ByteArrayOutputStream()
        val os = DataOutputStream(out)

        os.writeByte(1) // Starting frames
        os.writeShort(animation.getFrameList().size)

        for (frame in animation.getFrameList()) {
            os.writeShort(frame.getLength().toInt())
        }

        for (frame in animation.getFrameList()) {
            os.writeShort(frame.idProperty.get() and 0xFFFF)
        }

        for (frame in animation.getFrameList()) {
            os.writeShort(frame.idProperty.get() ushr 16)
        }

        // Other sequence attributes
        if (animation.loopOffsetProperty.get() != -1) {
            os.writeByte(2)
            os.writeShort(animation.loopOffsetProperty.get())
        }
        if (animation.leftHandItemProperty.get() != -1) {
            os.writeByte(6)
            os.writeShort(animation.leftHandItemProperty.get())
        }
        if (animation.rightHandItemProperty.get() != -1) {
            os.writeByte(7)
            os.writeShort(animation.rightHandItemProperty.get())
        }

        os.writeByte(0) // End of definition
        os.close()
        return out.toByteArray()
    }

    private fun encodeFrame(animationFrame: AnimationFrame): ByteArray {
        val out = ByteArrayOutputStream()
        val os = DataOutputStream(out)

        os.writeShort(animationFrame.definition?.transformationGroup?.id!!)

        val length = animationFrame.transformationList.maxByOrNull { it.idProperty.get() }?.idProperty?.get()?:0

        os.writeByte(length + 1)

        var index = 0
        for (transformation in animationFrame.transformationList) {
            repeat(transformation.idProperty.get() - index) {
                os.write(0)
            }
            index = transformation.idProperty.get() + 1
            val mask = getMask(transformation)
            os.writeByte(mask)
        }

        for (transformation in animationFrame.transformationList) {
            val mask = getMask(transformation)
            if (mask == 0)
                continue

            if (mask and 1 != 0) {
                writeSmartShort(os, transformation.getDeltaX())
            }

            if (mask and 2 != 0) {
                writeSmartShort(os, transformation.getDeltaY())
            }

            if (mask and 4 != 0) {
                writeSmartShort(os, transformation.getDeltaZ())
            }
        }

        os.close()
        return out.toByteArray()
    }

    private fun writeSmartShort(os: DataOutputStream, value: Int) {
        if (value >= -64 && value < 64) {
            os.writeByte(value + 64)
        } else if (value >= -16384 && value < 16384) {
            os.writeShort(value + 49152)
        }
    }


    fun getMask(transformation: Transformation): Int {
        val x = if (transformation.getDeltaX() != 0) 1 else 0
        val y = if (transformation.getDeltaY() != 0) 2 else 0
        val z = if (transformation.getDeltaZ() != 0) 4 else 0
        return x or y or z
    }
    private fun encodeFrameMap(def: AnimationSkeletonDefinition): ByteArray {
        val out = ByteArrayOutputStream()
        val os = DataOutputStream(out)

        os.writeShort(def.transformationTypes.size)

        for (transformationType in def.transformationTypes) {
            os.writeShort(transformationType)
        }

        for (vertexGroups in def.targetVertexGroupsIndices) {
            os.writeShort(vertexGroups.size)
        }

        for (vertexGroupsIndices in def.targetVertexGroupsIndices) {
            for (vertexGroupIndex in vertexGroupsIndices) {
                os.writeShort(vertexGroupIndex)
            }
        }

        os.close()
        return out.toByteArray()
    }

}