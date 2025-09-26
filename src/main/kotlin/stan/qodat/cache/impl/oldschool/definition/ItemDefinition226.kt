package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.ItemDefinition
import java.util.OptionalInt

class ItemDefinition226(private val id: Int) : ItemDefinition {

    var inventoryModel = -1

    var examineText: String? = null
    var zoom2d = 2000
    var xan2d = 0
    var yan2d = 0
    var xOffset2d = 0
    var yOffset2d = 0
    var unknown1: String? = null
    var stackable: Int = -1
    var cost = 1
    var wearPos1 = -1
    var wearPos2 = -1
    var members = false

    var maleModel0 = -1
    var maleOffset = 0
    var maleModel1 = -1

    var femaleModel0 = -1
    var femaleOffset = 0
    var femaleModel1 = -1

    var wearPos3 = -1

    var options : Array<String?>? = null
    var interfaceOptions : Array<String?>? = null

    var recolorToFind : ShortArray? = null
    var recolorToReplace : ShortArray? = null

    var retextureToFind : ShortArray? = null
    var retextureToReplace : ShortArray? = null

    var shiftClickDropIndex = -1

    var opId = -1
    var subOps: Array<Array<String?>?>? = null

    var isTradeable = false
    var weight = 0

    var maleModel2 = -1
    var femaleModel2 = -1

    var maleHeadModel = -1
    var femaleHeadModel = -1

    var maleHeadModel2 = -1
    var femaleHeadModel2 = -1

    var category = -1
    var zan2d = 0

    var notedId = -1
    var notedTemplateId = -1

    var countObj: IntArray? = null
    var countCo: IntArray? = null

    var resizeX = 128
    var resizeY = 128
    var resizeZ = 128

    var ambient = 0
    var contrast = 0

    var team = 0
    var boughtId = -1
    var boughtTemplateId = -1

    var placeholderId = -1
    var placeholderTemplateId = -1

    var params : HashMap<Int, Any>? = null


    override fun getOptionalId(): OptionalInt = OptionalInt.of(id)

    override var name: String = "$id"
    override val modelIds: Array<String> by lazy { arrayOf(inventoryModel.toString()) }
    override val findColor: ShortArray? = recolorToFind
    override val replaceColor: ShortArray? = recolorToReplace
}