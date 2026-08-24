package stan.qodat.cache.impl.oldschool.definition

/**
 * Fields shared by OSRS sequence revisions for opcodes 1–12 and 18.
 */
internal interface SequenceCommonFields {
    var frameIDs: IntArray?
    var chatFrameIds: IntArray?
    var frameLenghts: IntArray?
    var frameStep: Int
    var interleaveLeave: IntArray?
    var stretches: Boolean
    var forcedPriority: Int
    var leftHandItem: Int
    var rightHandItem: Int
    var maxLoops: Int
    var precedenceAnimating: Int
    var priority: Int
    var replyMode: Int
    var name: String?

    companion object {
        val EMPTY_INTS = IntArray(0)
    }
}
