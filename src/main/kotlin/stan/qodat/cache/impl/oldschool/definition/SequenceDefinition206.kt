package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationDefinition

class SequenceDefinition206(override val id: String) : AnimationDefinition {

    var frameIDs : IntArray? = null
    var chatFrameIds: IntArray? = null
    var frameLenghts: IntArray? = null
    var frameSounds: IntArray? = null
    var frameStep = -1
    var interleaveLeave: IntArray? = null
    var stretches = false
    var forcedPriority = 5
    var maxLoops = 99
    var precedenceAnimating = -1
    var priority = -1
    var replyMode = 2
    var cachedModelId = -1

    override val frameHashes: IntArray get() = frameIDs!!
    override val frameLengths: IntArray get() = frameLenghts!!
    override val loopOffset: Int get() = frameStep
    override var leftHandItem = -1
    override var rightHandItem = -1

}
