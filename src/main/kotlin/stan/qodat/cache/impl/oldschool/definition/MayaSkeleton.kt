package stan.qodat.cache.impl.oldschool.definition

import net.runelite.cache.io.InputStream

class MayaSkeleton() {

    lateinit var bones: Array<SkeletalBone>
    var poseCount: Int = -1

    constructor(stream: InputStream, boneCount: Int) : this() {
        poseCount = stream.readUnsignedByte()
        bones = Array(boneCount) { SkeletalBone(it, poseCount, false, stream) }
        linkBones()
    }

    private fun linkBones() {
        for (i in 0 until bones.size) {
            val bone = bones[i]
            if (bone.parentId >= 0) {
                bone.parent = bones[bone.parentId]
            }
        }
    }


}