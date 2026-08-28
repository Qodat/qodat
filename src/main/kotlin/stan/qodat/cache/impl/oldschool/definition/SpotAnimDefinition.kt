package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.SpotAnimationDefinition
import java.util.OptionalInt

class SpotAnimDefinition(val id: Int) : SpotAnimationDefinition {

    var modelId = 0
    var animationId = -1
    override var resizeX = 128
    override var resizeY = 128
    var rotation = 0
    var ambient = 0
    var contrast = 0
    var debugName: String? = null
    var recolorToFind: ShortArray? = null
    var recolorToReplace: ShortArray? = null
    var textureToFind: ShortArray? = null
    var textureToReplace: ShortArray? = null

    override fun getOptionalId(): OptionalInt = OptionalInt.of(id)
    override val name: String get() = id.toString()
    override val modelIds: Array<String> get() = arrayOf(modelId.toString())
    override val findColor: ShortArray? get() = recolorToFind
    override val replaceColor: ShortArray? get() = recolorToReplace
    override val animationIds: Array<String> get() = arrayOf(animationId.toString())
}
