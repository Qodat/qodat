package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.ObjectDefinition
import stan.qodat.cache.CacheIdStrings
import java.util.OptionalInt

class ObjectDefinition(val id: Int) : ObjectDefinition {

    override var name: String = "null"
    var objectModels: IntArray? = null
    var objectTypes: IntArray? = null
    var sizeX = 1
    var sizeY = 1
    var interactType = 2
    var blocksProjectile = true
    var wallOrDoor = -1
    var contouredGround = -1
    var mergeNormals = false
    var modelClipped = false
    var animationId = -1
    var decorDisplacement = 16
    var ambient = 0
    var contrast = 0
    var actions = arrayOfNulls<String>(5)
    val subOps = mutableListOf<ObjectSubOp>()
    val conditionalOps = mutableListOf<ObjectConditionalOp>()
    val conditionalSubOps = mutableListOf<ObjectConditionalSubOp>()
    var recolorToFind: ShortArray? = null
    var recolorToReplace: ShortArray? = null
    var retextureToFind: ShortArray? = null
    var textureToReplace: ShortArray? = null
    var category = 0
    var isRotated = false
    var shadow = true
    override var modelSizeX = 128
    override var modelSizeHeight = 128
    override var modelSizeY = 128
    var mapSceneID = -1
    var blockingMask = 0
    var offsetX = 0
    var offsetHeight = 0
    var offsetY = 0
    var obstructsGround = false
    var isHollow = false
    var supportsItems = -1
    var varbitId = -1
    var varpId = -1
    var configChangeDest: IntArray? = null
    var ambientSoundId = -1
    var ambientSoundDistance = 0
    var ambientSoundRetain = 0
    var ambientSoundIds: IntArray? = null
    var ambientSoundChangeTicksMin = 0
    var ambientSoundChangeTicksMax = 0
    var mapAreaId = -1
    var randomizeAnimStart = false
    var deferAnimChange = false
    var soundDistanceFadeCurve = 0
    var soundFadeInCurve = 0
    var soundFadeInDuration = 300
    var soundFadeOutCurve = 0
    var soundFadeOutDuration = 300
    var unknown1 = false
    var soundVisibility = 2
    var raise = 0
    var params: HashMap<Int, Any>? = null

    override fun getOptionalId(): OptionalInt = OptionalInt.of(id)
    override val modelIds: Array<String>
        get() {
            cachedModelIds?.let { return it }
            val ids = objectModels
            val mapped = if (ids == null || ids.isEmpty()) CacheIdStrings.EMPTY
            else CacheIdStrings.of(ids)
            cachedModelIds = mapped
            return mapped
        }
    override val findColor: ShortArray? get() = recolorToFind
    override val replaceColor: ShortArray? get() = recolorToReplace
    override val animationIds: Array<String>
        get() {
            cachedAnimationIds?.let { return it }
            val mapped = if (animationId == -1) CacheIdStrings.EMPTY
            else arrayOf(CacheIdStrings.of(animationId))
            cachedAnimationIds = mapped
            return mapped
        }

    @Transient private var cachedModelIds: Array<String>? = null
    @Transient private var cachedAnimationIds: Array<String>? = null
}

data class ObjectSubOp(val index: Int, val subId: Int, val text: String)

data class ObjectConditionalOp(
    val index: Int,
    val text: String,
    val varpId: Int,
    val varbitId: Int,
    val minValue: Int,
    val maxValue: Int,
)

data class ObjectConditionalSubOp(
    val index: Int,
    val subId: Int,
    val text: String,
    val varpId: Int,
    val varbitId: Int,
    val minValue: Int,
    val maxValue: Int,
)
