package qodat.cache.definition

import net.runelite.cache.definitions.SequenceDefinition

interface AnimationMayaDefinition : AnimationDefinition {

    val animMayaID : Int
    val animMayaFrameSounds: Map<Int, SequenceDefinition.Sound>
    val animMayaStart: Int
    val animMayaEnd: Int
    val animMayaMasks: BooleanArray

}
