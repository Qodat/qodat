package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationSound
import stan.qodat.cache.impl.oldschool.definition.SequenceCommonFields.Companion.EMPTY_INTS

class SequenceDefinition226(override val id: String) : AnimationDefinition, SequenceCommonFields {

    override var frameIDs: IntArray? = null
    override var chatFrameIds: IntArray? = null
    override var frameLenghts: IntArray? = null
    var frameSounds: IntArray? = null
    override var frameStep = -1
    override var interleaveLeave: IntArray? = null
    override var stretches = false
    override var forcedPriority = 5
    override var maxLoops = 99
    override var precedenceAnimating = -1
    override var priority = -1
    override var replyMode = 2
    var animMayaId = -1
    var animMayaStart = 0
    var animMayaEnd = 0
    var verticalOffset = 0
    var animMayaMasks: BooleanArray? = null
    override var name: String? = null
    var sounds: Map<Int, AnimationSound?>? = null
    var soundsCrossWorldView = false

    override val frameHashes: IntArray get() = frameIDs ?: EMPTY_INTS
    override val frameLengths: IntArray get() = frameLenghts ?: EMPTY_INTS
    override val loopOffset: Int get() = frameStep
    override var leftHandItem = -1
    override var rightHandItem = -1

}
