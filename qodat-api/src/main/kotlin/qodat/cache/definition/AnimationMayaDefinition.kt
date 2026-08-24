package qodat.cache.definition

interface AnimationMayaDefinition : AnimationDefinition {

    val animMayaID : Int
    val animMayaFrameSounds: Map<Int, AnimationSound>
    val animMayaStart: Int
    val animMayaEnd: Int
    val animMayaMasks: BooleanArray

}
