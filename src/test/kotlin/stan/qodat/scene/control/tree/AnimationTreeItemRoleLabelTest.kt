package stan.qodat.scene.control.tree

import qodat.cache.Cache
import qodat.cache.definition.AnimationDefinition
import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup
import qodat.cache.definition.InterfaceDefinition
import qodat.cache.definition.ItemDefinition
import qodat.cache.definition.ModelDefinition
import qodat.cache.definition.NPCDefinition
import qodat.cache.definition.ObjectDefinition
import qodat.cache.definition.SpotAnimationDefinition
import qodat.cache.definition.SpriteDefinition
import qodat.cache.definition.TextureDefinition
import stan.qodat.cache.impl.qodat.QodatAnimationDefinition
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.entity.NPC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimationTreeItemRoleLabelTest {

    @Test
    fun roleLabelUsesDefinitionIdWhenPresent() {
        val entity = npc(labels = mapOf("808" to "Idle", "819" to "Walk"))
        val animation = AnimationLegacy(
            "idle",
            QodatAnimationDefinition("808", intArrayOf(), intArrayOf(), -1, -1, -1)
        )
        animation.idProperty.set(0)
        assertEquals("Idle", AnimationTreeItem.roleLabel(entity, animation))
    }

    @Test
    fun roleLabelFallsBackToIdPropertyWhenDefinitionIsMissing() {
        val entity = npc(labels = mapOf("819" to "Walk"))
        val animation = AnimationLegacy("walk")
        animation.idProperty.set(819)
        assertEquals("Walk", AnimationTreeItem.roleLabel(entity, animation))
    }

    @Test
    fun roleLabelIsNullWhenIdentityIsUnknownOrUnlabelled() {
        val entity = npc(labels = mapOf("808" to "Idle"))
        val missing = AnimationLegacy("other")
        missing.idProperty.set(900)
        assertNull(AnimationTreeItem.roleLabel(entity, missing))
        assertNull(AnimationTreeItem.roleLabel(npc(), AnimationLegacy("idle")))
    }

    @Test
    fun extraAnimationCountIsNonPrimaryRemainder() {
        val entity = npc(
            animationIds = arrayOf("808", "819", "900"),
            primaryAnimationIds = arrayOf("808", "819")
        )
        assertEquals(1, entity.extraAnimationCount())
        assertEquals(0, npc(animationIds = arrayOf("808"), primaryAnimationIds = arrayOf("808", "819")).extraAnimationCount())
    }

    private fun npc(
        animationIds: Array<String> = arrayOf("808", "819"),
        primaryAnimationIds: Array<String> = animationIds,
        labels: Map<String, String> = emptyMap()
    ) = NPC(UnusedCache, FakeNpcDefinition(animationIds, primaryAnimationIds, labels)) { emptyArray() }

    private class FakeNpcDefinition(
        override val animationIds: Array<String>,
        override val primaryAnimationIds: Array<String>,
        override val animationRoleLabels: Map<String, String>
    ) : NPCDefinition {
        override val name = "Guard"
        override val modelIds = emptyArray<String>()
        override val findColor: ShortArray? = null
        override val replaceColor: ShortArray? = null
    }

    private object UnusedCache : Cache("test") {
        override fun getModelDefinition(id: String): ModelDefinition = error("unused")
        override fun getAnimation(id: String): AnimationDefinition = error("unused")
        override fun getNPCs(): Array<NPCDefinition> = emptyArray()
        override fun getObjects(): Array<ObjectDefinition> = emptyArray()
        override fun getItems(): Array<ItemDefinition> = emptyArray()
        override fun getSpotAnimations(): Array<SpotAnimationDefinition> = emptyArray()
        override fun getAnimationDefinitions(): Array<AnimationDefinition> = emptyArray()
        override fun getAnimationSkeletonDefinition(frameHash: Int): AnimationTransformationGroup = error("unused")
        override fun getFrameDefinition(frameHash: Int): AnimationFrameLegacyDefinition? = null
        override fun getInterface(groupId: Int): Array<InterfaceDefinition> = emptyArray()
        override fun getRootInterfaces(): Map<Int, List<InterfaceDefinition>> = emptyMap()
        override fun getSprites(): Array<SpriteDefinition> = emptyArray()
        override fun getSprite(groupId: Int, frameId: Int): SpriteDefinition = error("unused")
        override fun getTexture(id: Int): TextureDefinition = error("unused")
    }
}
