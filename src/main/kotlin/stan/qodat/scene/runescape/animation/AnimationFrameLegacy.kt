package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.onInvalidation

class AnimationFrameLegacy(
    name: String,
    val definition: AnimationFrameLegacyDefinition?,
    duration: Int
) : AnimationFrame(name, duration) {

    val transformationCountProperty = SimpleIntegerProperty(definition?.transformationCount?:0)
    val transformationList = FXCollections.observableArrayList<Transformation>()

    init {

        if (definition != null) {
            val transformations = Array(transformationCountProperty.get()) {
                val groupIndex = definition.transformationGroupAccessIndices[it]
                Transformation(
                    "transform[$it]",
                    definition.transformationGroup.targetVertexGroupsIndices[groupIndex],
                    definition.transformationGroup.transformationTypes[groupIndex],
                    definition.transformationDeltaX[it],
                    definition.transformationDeltaY[it],
                    definition.transformationDeltaZ[it]
                ).apply {
                    idProperty.set(it)
                    groupIndexProperty.set(groupIndex)
                }
            }
            transformationList.addAll(transformations)
        }
        transformationList.onInvalidation {
            transformationCountProperty.set(transformationList.size)
        }
    }
    override fun getLength() = FrameTimeUtil.toFrame(durationProperty.get())

    fun getTransformationCount() = transformationCountProperty.get()
    fun clone(name: String) = AnimationFrameLegacy(
        name = name,
        definition = object : AnimationFrameLegacyDefinition {
            override val transformationCount: Int = transformationCountProperty.get()
            override val transformationGroupAccessIndices: IntArray =
                transformationList.map { it.groupIndexProperty.get() }.toIntArray()
            override val transformationDeltaX: IntArray =
                transformationList.map { it.getDeltaX() }.toIntArray()
            override val transformationDeltaY: IntArray =
                transformationList.map { it.getDeltaY() }.toIntArray()
            override val transformationDeltaZ: IntArray =
                transformationList.map { it.getDeltaZ() }.toIntArray()
            override val transformationGroup: AnimationTransformationGroup =
                definition!!.transformationGroup
        },
        duration = getLength().toInt()
    ).apply {
        val hex = Integer.toHexString(this@AnimationFrameLegacy.idProperty.get())
        val fileId = getFileId(hex)
        val frameId = getFrameId(hex) + 1
        this.idProperty.set(((fileId and  0xFFFF) shl 16) or (frameId and 0xFFFF))
    }

}
