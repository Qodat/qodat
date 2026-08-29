package stan.qodat.scene.control.pose

import stan.qodat.scene.runescape.model.ModelSkeleton

/**
 * Per-vertex-group pose deltas, applied as FK after bind pose / animation.
 * Rotation uses RS units (`(value & 255) * 8` in [ModelSkeleton]).
 */
class ModelPose {

    private val bones = HashMap<Int, BoneDelta>()

    fun isEmpty(): Boolean = bones.values.all { it.isIdentity() }

    fun delta(groupIndex: Int): BoneDelta =
        bones.getOrPut(groupIndex) { BoneDelta() }

    fun deltaOrNull(groupIndex: Int): BoneDelta? = bones[groupIndex]

    fun clear() {
        bones.clear()
    }

    fun addTranslate(groupIndex: Int, dx: Int, dy: Int, dz: Int) {
        if (dx == 0 && dy == 0 && dz == 0)
            return
        delta(groupIndex).apply {
            tx += dx
            ty += dy
            tz += dz
        }
    }

    fun addRotate(groupIndex: Int, dx: Int, dy: Int, dz: Int) {
        if (dx == 0 && dy == 0 && dz == 0)
            return
        delta(groupIndex).apply {
            rx = (rx + dx) and 255
            ry = (ry + dy) and 255
            rz = (rz + dz) and 255
        }
    }

    class BoneDelta {
        var rx: Int = 0
        var ry: Int = 0
        var rz: Int = 0
        var tx: Int = 0
        var ty: Int = 0
        var tz: Int = 0

        fun isIdentity(): Boolean =
            rx == 0 && ry == 0 && rz == 0 && tx == 0 && ty == 0 && tz == 0
    }
}

data class PoseBone(
    val groupIndex: Int,
    val parentGroupIndex: Int?,
    val descendantGroups: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoseBone) return false
        return groupIndex == other.groupIndex &&
            parentGroupIndex == other.parentGroupIndex &&
            descendantGroups.contentEquals(other.descendantGroups)
    }

    override fun hashCode(): Int =
        31 * (31 * groupIndex + (parentGroupIndex ?: 0)) + descendantGroups.contentHashCode()
}

data class PoseSkeleton(val bones: List<PoseBone>) {
    fun bone(groupIndex: Int): PoseBone? = bones.firstOrNull { it.groupIndex == groupIndex }
}

/**
 * Applies [pose] parent-first: each non-identity bone uses its own centroid as
 * pivot, then rotates / translates that group plus descendants.
 */
fun ModelSkeleton.applyPose(pose: ModelPose, skeleton: PoseSkeleton) {
    if (pose.isEmpty() || skeleton.bones.isEmpty())
        return
    for (bone in skeleton.bones) {
        val delta = pose.deltaOrNull(bone.groupIndex) ?: continue
        if (delta.isIdentity())
            continue
        applyBonePose(bone, delta)
    }
}
