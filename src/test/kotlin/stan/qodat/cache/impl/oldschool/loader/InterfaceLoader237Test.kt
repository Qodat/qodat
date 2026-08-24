package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.ClientScript1Instruction
import com.displee.io.impl.OutputBuffer
import stan.qodat.cache.impl.oldschool.definition.RuneliteInterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import qodat.cache.definition.ClientScript1Instruction as QodatScript

class InterfaceLoader237Test {

    @Test
    fun hasRev237MagicRequiresPrefixAndExtraBytes() {
        assertFalse(InterfaceLoader237.hasRev237Magic(byteArrayOf()))
        assertFalse(InterfaceLoader237.hasRev237Magic(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())))
        assertTrue(
            InterfaceLoader237.hasRev237Magic(
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0)
            )
        )
        assertFalse(
            InterfaceLoader237.hasRev237Magic(
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDE.toByte(), 0)
            )
        )
    }

    @Test
    fun decodesIf1LayerAndRemapsParent() {
        val groupId = 3
        val childId = 7
        val widgetId = (groupId shl 16) + childId
        val bytes = OutputBuffer(16).apply {
            writeIf1Header(
                type = 0,
                menuType = 0,
                originalX = 10,
                originalY = 20,
                originalWidth = 100,
                originalHeight = 50,
                parentId = 4,
                hoveredSiblingId = 0xFFFF,
            )
            writeShort(200)
            writeByte(1)
        }.array()

        val iface = InterfaceLoader237().load(widgetId, bytes)
        assertFalse(iface.isIf3)
        assertEquals(widgetId, iface.id)
        assertEquals(0, iface.type)
        assertEquals(10, iface.originalX)
        assertEquals(20, iface.originalY)
        assertEquals(100, iface.originalWidth)
        assertEquals(50, iface.originalHeight)
        assertEquals((groupId shl 16) + 4, iface.parentId)
        assertEquals(-1, iface.hoveredSiblingId)
        assertEquals(200, iface.scrollHeight)
        assertTrue(iface.isHidden)
    }

    @Test
    fun if1UnsetParentBecomesMinusOne() {
        val bytes = OutputBuffer(16).apply {
            writeIf1Header(type = 0, parentId = 0xFFFF)
            writeShort(0)
            writeByte(0)
        }.array()

        val iface = InterfaceLoader237().load(1, bytes)
        assertEquals(-1, iface.parentId)
    }

    @Test
    fun decodesIf1TextAndDefaultTooltip() {
        val bytes = OutputBuffer(16).apply {
            writeIf1Header(type = 4, menuType = 1)
            writeByte(1)
            writeByte(2)
            writeByte(3)
            writeShort(9)
            writeByte(1)
            writeString("Hello")
            writeString("Alt")
            writeInt(0x00FF00)
            writeInt(0x0000FF)
            writeInt(0xFF0000)
            writeInt(0x111111)
            writeString("")
        }.array()

        val iface = InterfaceLoader237().load(2, bytes)
        assertEquals("Hello", iface.text)
        assertEquals("Alt", iface.alternateText)
        assertEquals(9, iface.fontId)
        assertTrue(iface.textShadowed)
        assertEquals(0x00FF00, iface.textColor)
        assertEquals("Ok", iface.tooltip)
        assertEquals(4194304, iface.clickMask and 4194304)
    }

    @Test
    fun decodesIf1ClientScripts() {
        val bytes = OutputBuffer(16).apply {
            writeIf1Header(type = 0, scriptCount = 1)
            writeShort(3)
            writeShort(ClientScript1Instruction.Opcode.CONSTANT.ordinal)
            writeShort(42)
            writeShort(ClientScript1Instruction.Opcode.RETURN.ordinal)
            writeShort(0)
            writeByte(0)
        }.array()

        val iface = InterfaceLoader237().load(8, bytes)
        val script = iface.clientScripts[0]
        assertEquals(ClientScript1Instruction.Opcode.CONSTANT, script[0].opcode)
        assertTrue(script[0].operands.contentEquals(intArrayOf(42)))
        assertEquals(ClientScript1Instruction.Opcode.RETURN, script[1].opcode)

        val mapped = RuneliteInterfaceDefinition(iface)
        assertEquals(QodatScript.Opcode.CONSTANT, mapped.clientScripts!![0][0].opcode)
        assertTrue(mapped.clientScripts!![0][0].operands.contentEquals(intArrayOf(42)))
        assertEquals(QodatScript.Opcode.RETURN, mapped.clientScripts!![0][1].opcode)
    }

    @Test
    fun decodesIf3LayerAndListeners() {
        val bytes = OutputBuffer(16).apply {
            writeByte(0xFF)
            writeIf3Common(type = 0)
            writeShort(16)
            writeShort(32)
            writeByte(1)
            writeIf3Tail(
                name = "panel",
                actions = arrayOf("Close"),
                onLoad = {
                    writeByte(2)
                    writeByte(0)
                    writeInt(99)
                    writeByte(1)
                    writeString("cb")
                },
                varTriggers = intArrayOf(5, 6),
            )
        }.array()

        val iface = InterfaceLoader237().load(16, bytes)
        assertTrue(iface.isIf3)
        assertEquals("panel", iface.name)
        assertEquals(16, iface.scrollWidth)
        assertEquals(32, iface.scrollHeight)
        assertTrue(iface.noClickThrough)
        assertTrue(iface.actions.contentEquals(arrayOf("Close")))
        assertEquals(99, iface.onLoadListener[0])
        assertEquals("cb", iface.onLoadListener[1])
        assertTrue(iface.hasListener)
        assertTrue(iface.varTransmitTriggers.contentEquals(intArrayOf(5, 6)))
        assertNull(iface.onClickListener)
    }

    @Test
    fun rev237If3ModelUsesIntId() {
        val vanilla = OutputBuffer(16).apply {
            writeByte(0xFF)
            writeIf3Common(type = 6, widthMode = 1)
            writeShort(0xFFFF)
            writeShort(1)
            writeShort(2)
            writeShort(3)
            writeShort(4)
            writeShort(5)
            writeShort(6)
            writeShort(0xFFFF)
            writeByte(1)
            writeShort(0)
            writeShort(77)
            writeIf3Tail()
        }.array()
        val vanillaIface = InterfaceLoader237().load(20, vanilla)
        assertEquals(-1, vanillaIface.modelId)
        assertEquals(1, vanillaIface.modelType)
        assertEquals(77, vanillaIface.modelHeightOverride)
        assertTrue(vanillaIface.orthogonal)
        assertFalse(InterfaceLoader237.hasRev237Magic(vanilla))

        val rev237 = OutputBuffer(16).apply {
            writeByte(0xAA)
            writeByte(0xBB)
            writeByte(0xCC)
            writeByte(0xDD)
            writeByte(0xFF)
            writeIf3Common(type = 6, widthMode = 1)
            writeInt(1_000_099)
            writeShort(0)
            writeShort(0)
            writeShort(0)
            writeShort(0)
            writeShort(0)
            writeShort(128)
            writeShort(0xFFFF)
            writeByte(0)
            writeShort(0)
            writeShort(8)
            writeShort(9)
            writeIf3Tail(name = "model")
        }.array()

        val iface = InterfaceLoader237().load(21, rev237)
        assertTrue(InterfaceLoader237.hasRev237Magic(rev237))
        assertTrue(iface.isIf3)
        assertEquals(1_000_099, iface.modelId)
        assertEquals(8, iface.modelHeightOverride)
        assertEquals("model", iface.name)
    }

    @Test
    fun mapsLoadedIf3ThroughRuneliteInterfaceDefinition() {
        val bytes = OutputBuffer(16).apply {
            writeByte(0xFF)
            writeIf3Common(type = 4, originalX = 3, originalY = 4)
            writeShort(12)
            writeString("Title")
            writeByte(14)
            writeByte(1)
            writeByte(2)
            writeByte(1)
            writeInt(0xABCDEF)
            writeIf3Tail(name = "label")
        }.array()

        val mapped = RuneliteInterfaceDefinition(InterfaceLoader237().load(30, bytes))
        assertTrue(mapped.isIf3)
        assertEquals(4, mapped.type)
        assertEquals("Title", mapped.text)
        assertEquals(12, mapped.fontId)
        assertEquals(0xABCDEF, mapped.textColor)
        assertEquals("label", mapped.name)
        assertEquals(3, mapped.originalX)
        assertEquals(4, mapped.originalY)
    }

    private fun OutputBuffer.writeIf1Header(
        type: Int,
        menuType: Int = 0,
        originalX: Int = 0,
        originalY: Int = 0,
        originalWidth: Int = 1,
        originalHeight: Int = 1,
        parentId: Int = 0xFFFF,
        hoveredSiblingId: Int = 0xFFFF,
        scriptCount: Int = 0,
    ) {
        writeByte(type)
        writeByte(menuType)
        writeShort(0)
        writeShort(originalX)
        writeShort(originalY)
        writeShort(originalWidth)
        writeShort(originalHeight)
        writeByte(0)
        writeShort(parentId)
        writeShort(hoveredSiblingId)
        writeByte(0)
        writeByte(scriptCount)
    }

    private fun OutputBuffer.writeIf3Common(
        type: Int,
        originalX: Int = 0,
        originalY: Int = 0,
        originalWidth: Int = 8,
        originalHeight: Int = 8,
        widthMode: Int = 0,
        heightMode: Int = 0,
        parentId: Int = 0xFFFF,
    ) {
        writeByte(type)
        writeShort(0)
        writeShort(originalX)
        writeShort(originalY)
        writeShort(originalWidth)
        writeShort(originalHeight)
        writeByte(widthMode)
        writeByte(heightMode)
        writeByte(0)
        writeByte(0)
        writeShort(parentId)
        writeByte(0)
    }

    private fun OutputBuffer.writeIf3Tail(
        name: String? = null,
        actions: Array<String> = emptyArray(),
        onLoad: (OutputBuffer.() -> Unit)? = null,
        varTriggers: IntArray = intArrayOf(),
    ) {
        write24BitInt(0)
        writeString(name ?: "")
        writeByte(actions.size)
        actions.forEach { writeString(it) }
        writeByte(0)
        writeByte(0)
        writeByte(0)
        writeString("")
        if (onLoad != null) onLoad() else writeByte(0)
        repeat(17) { writeByte(0) }
        if (varTriggers.isEmpty()) {
            writeByte(0)
        } else {
            writeByte(varTriggers.size)
            varTriggers.forEach { writeInt(it) }
        }
        writeByte(0)
        writeByte(0)
    }
}
