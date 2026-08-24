package stan.qodat.cache.impl.oldschool.definition

import net.runelite.cache.definitions.ClientScript1Instruction
import net.runelite.cache.definitions.InterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import qodat.cache.definition.ClientScript1Instruction as QodatScript

class RuneliteInterfaceDefinitionTest {

    @Test
    fun copiesScalarAndArrayFields() {
        val src = InterfaceDefinition().apply {
            id = 0x00010002
            isIf3 = true
            type = 5
            contentType = 3
            originalX = 4
            originalY = 5
            originalWidth = 6
            originalHeight = 7
            spriteId = 88
            textureId = 9
            spriteTiling = true
            opacity = 40
            modelId = 100
            text = "Hi"
            name = "icon"
            actions = arrayOf("Use", "Examine")
            hasListener = true
            tooltip = "Select"
            itemIds = intArrayOf(1, 2)
            configActions = arrayOf("A")
        }

        val mapped = RuneliteInterfaceDefinition(src)
        assertEquals(0x00010002, mapped.id)
        assertTrue(mapped.isIf3)
        assertEquals(5, mapped.type)
        assertEquals(3, mapped.contentType)
        assertEquals(4, mapped.originalX)
        assertEquals(5, mapped.originalY)
        assertEquals(6, mapped.originalWidth)
        assertEquals(7, mapped.originalHeight)
        assertEquals(88, mapped.spriteId)
        assertEquals(9, mapped.textureId)
        assertTrue(mapped.spriteTiling)
        assertEquals(40, mapped.opacity)
        assertEquals(100, mapped.modelId)
        assertEquals("Hi", mapped.text)
        assertEquals("icon", mapped.name)
        assertTrue(mapped.actions!!.contentEquals(arrayOf("Use", "Examine")))
        assertTrue(mapped.hasListener)
        assertEquals("Select", mapped.tooltip)
        assertTrue(mapped.itemIds!!.contentEquals(intArrayOf(1, 2)))
        assertTrue(mapped.configActions!!.contentEquals(arrayOf("A")))
    }

    @Test
    fun mapsKnownClientScriptOpcodesAndLeavesNullScriptsAlone() {
        assertNull(RuneliteInterfaceDefinition(InterfaceDefinition()).clientScripts)

        val src = InterfaceDefinition()
        src.clientScripts = arrayOf(
            arrayOf(
                instruction(ClientScript1Instruction.Opcode.VARP, 3),
                instruction(ClientScript1Instruction.Opcode.VARBIT, 7),
                instruction(ClientScript1Instruction.Opcode.MINUS),
                instruction(ClientScript1Instruction.Opcode.WORLD_Y, 1),
                instruction(ClientScript1Instruction.Opcode.CONSTANT, 99),
            )
        )

        val mapped = RuneliteInterfaceDefinition(src).clientScripts!![0]
        assertEquals(QodatScript.Opcode.VARP, mapped[0].opcode)
        assertTrue(mapped[0].operands.contentEquals(intArrayOf(3)))
        assertEquals(QodatScript.Opcode.VARBIT, mapped[1].opcode)
        assertEquals(QodatScript.Opcode.MINUS, mapped[2].opcode)
        assertEquals(QodatScript.Opcode.WORLD_Y, mapped[3].opcode)
        assertEquals(QodatScript.Opcode.CONSTANT, mapped[4].opcode)
        assertTrue(mapped[4].operands.contentEquals(intArrayOf(99)))
    }

    private fun instruction(
        opcode: ClientScript1Instruction.Opcode,
        vararg operands: Int,
    ): ClientScript1Instruction {
        val instruction = ClientScript1Instruction()
        instruction.opcode = opcode
        instruction.operands = operands
        return instruction
    }
}
