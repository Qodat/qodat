package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.NPCDefinition
import stan.qodat.cache.CacheIdStrings
import stan.qodat.cache.NpcPrimaryAnimations
import java.util.OptionalInt

class NpcDefinition(val id: Int) : NPCDefinition {

    override var name: String = NULL_NAME
    var size = 1
    var models: IntArray? = null
    var chatheadModels: IntArray? = null
    var standingAnimation = -1
    var idleRotateLeftAnimation = -1
    var idleRotateRightAnimation = -1
    var walkingAnimation = -1
    var rotate180Animation = -1
    var rotateLeftAnimation = -1
    var rotateRightAnimation = -1
    var runAnimation = -1
    var runRotate180Animation = -1
    var runRotateLeftAnimation = -1
    var runRotateRightAnimation = -1
    var crawlAnimation = -1
    var crawlRotate180Animation = -1
    var crawlRotateLeftAnimation = -1
    var crawlRotateRightAnimation = -1
    var idleAnimRestart = false
    var recolorToFind: ShortArray? = null
    var recolorToReplace: ShortArray? = null
    var retextureToFind: ShortArray? = null
    var retextureToReplace: ShortArray? = null
    var actions: Array<String?> = EMPTY_ACTIONS
    var subOps: List<NpcSubOp> = emptyList()
    var conditionalOps: List<NpcConditionalOp> = emptyList()
    var conditionalSubOps: List<NpcConditionalSubOp> = emptyList()
    var isMinimapVisible = true
    var combatLevel = -1
    override var widthScale = 128
    override var heightScale = 128
    var renderPriority = 0
    var ambient = 0
    var contrast = 0
    var headIconArchiveIds: IntArray? = null
    var headIconSpriteIndex: ShortArray? = null
    var rotationSpeed = 32
    var configs: IntArray? = null
    var varbitId = -1
    var varpIndex = -1
    var isInteractable = true
    var rotationFlag = true
    var isFollower = false
    var lowPriorityFollowerOps = false
    var params: HashMap<Int, Any>? = null
    var category = 0
    var height = -1
    var stats = intArrayOf(1, 1, 1, 1, 1, 1)
    var footprintSize = -1
    var canHideForOverlap = false
    var overlapTintHSL = 39188
    var unknown1 = false
    var zbuf = true

    override fun getOptionalId(): OptionalInt = OptionalInt.of(id)
    override val modelIds: Array<String>
        get() = models?.let { CacheIdStrings.of(it) } ?: CacheIdStrings.EMPTY
    override val findColor: ShortArray? get() = recolorToFind
    override val replaceColor: ShortArray? get() = recolorToReplace
    override val primaryAnimationIds: Array<String> get() = NpcPrimaryAnimations.ids(this)
    override val animationRoleLabels: Map<String, String> get() = NpcPrimaryAnimations.labels(this)
    override val animationIds: Array<String> get() = primaryAnimationIds

    companion object {
        const val NULL_NAME = "null"
        val EMPTY_ACTIONS: Array<String?> = arrayOfNulls(5)
    }
}

data class NpcSubOp(val index: Int, val subId: Int, val text: String)

data class NpcConditionalOp(
    val index: Int,
    val text: String,
    val varpId: Int,
    val varbitId: Int,
    val minValue: Int,
    val maxValue: Int,
)

data class NpcConditionalSubOp(
    val index: Int,
    val subId: Int,
    val text: String,
    val varpId: Int,
    val varbitId: Int,
    val minValue: Int,
    val maxValue: Int,
)
