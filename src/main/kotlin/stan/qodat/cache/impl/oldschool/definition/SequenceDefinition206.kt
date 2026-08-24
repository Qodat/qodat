package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationDefinition

class SequenceDefinition206(override val id: String) : AnimationDefinition, SequenceCommonFields {

    override var frameIDs : IntArray? = null
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
    override var name: String? = null

    override val frameHashes: IntArray get() = frameIDs!!
    override val frameLengths: IntArray get() = frameLenghts!!
    override val loopOffset: Int get() = frameStep
    override var leftHandItem = -1
    override var rightHandItem = -1

}
