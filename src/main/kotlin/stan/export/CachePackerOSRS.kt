//package stan.export
//
//import api.animation.IAnimation
//import api.animation.IKeyframe
//import api.animation.getMask
//import api.cache.ICacheLibrary
//import api.cache.ICachePacker
//import api.definition.SequenceDefinition
//import java.io.ByteArrayOutputStream
//import java.io.DataOutputStream
//
//class CachePackerOSRS : ICachePacker {
//
//    override fun toString() = "OSRS"
//
//    override fun packFrames(library: ICacheLibrary, animation: IAnimation): Int {
//        val archiveId = library.getLastArchiveId(FRAME_INDEX) + 1
//        library.addArchive(FRAME_INDEX, archiveId)
//
//        var id = 0 // To decrement keyframe id's if necessary
//        val archive = library.getArchive(FRAME_INDEX, archiveId)
//        animation.keyframes.forEach {
//            if (it.modified) {
//                archive?.addFile(id++, encodeKeyframe(it))
//            }
//        }
//        return archiveId
//    }
//
//    override fun packSequence(library: ICacheLibrary, sequence: SequenceDefinition) {
//        val data = encodeSequence(sequence)
//        library.getArchive(CONFIG_INDEX, SEQUENCE_INDEX)?.addFile(sequence.id, data)
//    }
//
//    private fun encodeKeyframe(keyframe: IKeyframe): ByteArray {
//        val out = ByteArrayOutputStream()
//        val os = DataOutputStream(out)
//
//        os.writeShort(keyframe.frameMap.id)
//        val length = keyframe.transformations.maxByOrNull { it.id }?.id ?: 0
//        os.writeByte(length + 1)
//
//        // Write masks first
//        var index = 0
//        for (transformation in keyframe.transformations) {
//            // Insert ignored masks to preserve transformation indices
//            repeat(transformation.id - index) {
//                os.writeByte(0)
//            }
//            index = transformation.id + 1
//
//            val mask = getMask(transformation.delta)
//            os.writeByte(mask)
//        }
//
//        // Write transformation values
//        for (transformation in keyframe.transformations) {
//            val mask = getMask(transformation.delta)
//            if (mask == 0) {
//                continue
//            }
//
//            if (mask and 1 != 0) {
//                writeSmartShort(os, transformation.delta.x)
//            }
//
//            if (mask and 2 != 0) {
//                writeSmartShort(os, transformation.delta.y)
//            }
//
//            if (mask and 4 != 0) {
//                writeSmartShort(os, transformation.delta.z)
//            }
//        }
//        os.close()
//        return out.toByteArray()
//    }
//
//    private fun writeSmartShort(os: DataOutputStream, value: Int) {
//        if (value >= -64 && value < 64) {
//            os.writeByte(value + 64)
//        } else if (value >= -16384 && value < 16384) {
//            os.writeShort(value + 49152)
//        }
//    }
//
//    private fun encodeSequence(sequence: SequenceDefinition): ByteArray {
//        val out = ByteArrayOutputStream()
//        val os = DataOutputStream(out)
//
//        os.writeByte(1) // Starting frames
//        os.writeShort(sequence.frameIds.size)
//
//        for (length in sequence.frameLengths) {
//            os.writeShort(length)
//        }
//
//        for (frameId in sequence.frameIds) {
//            os.writeShort(frameId and 0xFFFF)
//        }
//
//        for (frameId in sequence.frameIds) {
//            os.writeShort(frameId ushr 16)
//        }
//
//        // Other sequence attributes
//        if (sequence.loopOffset != -1) {
//            os.writeByte(2)
//            os.writeShort(sequence.loopOffset)
//        }
//        if (sequence.leftHandItem != -1) {
//            os.writeByte(6)
//            os.writeShort(sequence.leftHandItem)
//        }
//        if (sequence.rightHandItem != -1) {
//            os.writeByte(7)
//            os.writeShort(sequence.rightHandItem)
//        }
//
//        os.writeByte(0) // End of definition
//        os.close()
//        return out.toByteArray()
//    }
//
//    override val sequenceConfigIndex = CONFIG_INDEX
//}