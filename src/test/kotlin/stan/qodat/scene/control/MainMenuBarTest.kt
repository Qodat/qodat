package stan.qodat.scene.control

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.JFXPanel
import javafx.event.ActionEvent
import javafx.scene.SceneAntialiasing
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainMenuBarTest {

    init {
        JFXPanel()
    }

    @Test
    fun installKeepsTheMenuBarInsideTheWindow() {
        val menuBar = MenuBar()
        MainMenuBar.install(menuBar, recordingActions(), viewToggles())
        assertFalse(menuBar.isUseSystemMenuBar)
        assertEquals(4, menuBar.menus.size)
    }

    @Test
    fun installBuildsFileEditViewAndHelpMenus() {
        val menus = MainMenuBar.buildMenus(recordingActions(), viewToggles())

        assertEquals(listOf("_File", "_Edit", "_View", "_Help"), menus.map { it.text })
        assertTrue(menus.all { it.isMnemonicParsing })
    }

    @Test
    fun fileMenuWiresCacheSettingsAndQuitActions() {
        val invoked = mutableListOf<String>()
        val file = MainMenuBar.buildMenus(recordingActions(invoked), viewToggles())[0]
        assertEquals(
            listOf(
                "Change Cache…",
                "Reload Cache",
                "Rescan Animations",
                "-",
                "Open .qodat Folder",
                "-",
                "Settings…",
                "-",
                "Quit"
            ),
            file.items.map { if (it is SeparatorMenuItem) "-" else it.text }
        )

        file.items.filterIsInstance<MenuItem>()
            .filter { it !is SeparatorMenuItem && it.text != null }
            .forEach { item ->
                assertNotNull(item.graphic, "${item.text} should have an icon")
            }

        fire(file, "Settings…")
        fire(file, "Reload Cache")
        fire(file, "Quit")
        assertEquals(listOf("settings", "reload", "quit"), invoked)
    }

    @Test
    fun fileAndEditMenusUseStandardAccelerators() {
        val menus = MainMenuBar.buildMenus(recordingActions(), viewToggles())
        val file = menus[0]
        assertEquals(combo(KeyCode.O, shift = true), item(file, "Change Cache…").accelerator)
        assertEquals(combo(KeyCode.R), item(file, "Reload Cache").accelerator)
        assertEquals(combo(KeyCode.COMMA), item(file, "Settings…").accelerator)
        assertEquals(combo(KeyCode.Q), item(file, "Quit").accelerator)

        val edit = menus[1]
        assertEquals(combo(KeyCode.Z), item(edit, "Undo").accelerator)
        assertEquals(combo(KeyCode.Z, shift = true), item(edit, "Redo").accelerator)
    }

    @Test
    fun viewMenuTogglesStayBoundToProperties() {
        val showAxis = SimpleBooleanProperty(false)
        val menus = MainMenuBar.buildMenus(recordingActions(), viewToggles(showAxis = showAxis))

        val item = menus[2].items
            .filterIsInstance<CheckMenuItem>()
            .first { it.text == "Show Axis" }

        assertFalse(item.isSelected)
        item.isSelected = true
        assertTrue(showAxis.get())
        showAxis.set(false)
        assertFalse(item.isSelected)
    }

    @Test
    fun menuIconsAreSizedForTheMenuBar() {
        val icon = MenuIcons.settings()
        assertEquals(14.0, icon.prefWidth)
        assertEquals(14.0, icon.prefHeight)
        assertTrue(icon.styleClass.contains("menu-icon"))
        assertTrue(icon.style.contains("-fx-shape"))
    }

    private fun viewToggles(
        showAxis: SimpleBooleanProperty = SimpleBooleanProperty(true)
    ) = MainMenuBar.ViewToggles(
        showAxis = showAxis,
        showFps = SimpleBooleanProperty(true),
        showUnnamedEntities = SimpleBooleanProperty(true),
        renderTextures = SimpleBooleanProperty(false),
        antialiasing = SimpleObjectProperty(SceneAntialiasing.BALANCED),
        invertCamera = SimpleBooleanProperty(true)
    )

    private fun recordingActions(invoked: MutableList<String> = mutableListOf()) =
        MainMenuBar.Actions(
            changeCache = { invoked += "cache" },
            reloadCache = { invoked += "reload" },
            rescanAnimations = { invoked += "rescan" },
            openQodatFolder = { invoked += "folder" },
            openSettings = { invoked += "settings" },
            quit = { invoked += "quit" },
            undo = { invoked += "undo" },
            redo = { invoked += "redo" },
            clearScene = { invoked += "clear" },
            showAbout = { invoked += "about" }
        )

    private fun fire(menu: Menu, text: String) {
        item(menu, text).onAction.handle(ActionEvent())
    }

    private fun item(menu: Menu, text: String): MenuItem =
        menu.items.first { it.text == text }

    private fun combo(code: KeyCode, shift: Boolean = false): KeyCombination =
        if (shift)
            KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
        else
            KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN)
}
