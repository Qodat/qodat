package stan.qodat.scene.control

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.scene.SceneAntialiasing
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Region
import stan.qodat.Properties
import stan.qodat.util.setAndBind

/**
 * Builds the application menu bar: cache actions, undo/redo, view toggles, and settings.
 */
object MainMenuBar {

    data class Actions(
        val changeCache: () -> Unit,
        val reloadCache: () -> Unit,
        val rescanAnimations: () -> Unit,
        val openQodatFolder: () -> Unit,
        val openSettings: () -> Unit,
        val quit: () -> Unit,
        val undo: () -> Unit,
        val redo: () -> Unit,
        val clearScene: () -> Unit,
        val checkForUpdates: () -> Unit,
        val showAbout: () -> Unit
    )

    data class ViewToggles(
        val showAxis: BooleanProperty,
        val showFps: BooleanProperty,
        val showUnnamedEntities: BooleanProperty,
        val renderTextures: BooleanProperty,
        val antialiasing: ObjectProperty<SceneAntialiasing>,
        val invertCamera: BooleanProperty
    ) {
        companion object {
            fun fromProperties() = ViewToggles(
                showAxis = Properties.showAxis,
                showFps = Properties.showFPS,
                showUnnamedEntities = Properties.showNullNamedEntities,
                renderTextures = Properties.alwaysRenderUsingAtlas,
                antialiasing = Properties.antialiasing,
                invertCamera = Properties.cameraInvert
            )
        }
    }

    fun install(
        menuBar: MenuBar,
        actions: Actions,
        viewToggles: ViewToggles
    ) {
        menuBar.isUseSystemMenuBar = false
        menuBar.menus.setAll(buildMenus(actions, viewToggles))
    }

    fun buildMenus(actions: Actions, viewToggles: ViewToggles): List<Menu> = listOf(
        fileMenu(actions),
        editMenu(actions),
        viewMenu(viewToggles),
        helpMenu(actions)
    )

    private fun fileMenu(actions: Actions) = Menu("_File").apply {
        isMnemonicParsing = true
        items.setAll(
            item("Change Cache…", MenuIcons.folder(), actions.changeCache, key(KeyCode.O, shift = true)),
            item("Reload Cache", MenuIcons.reload(), actions.reloadCache, key(KeyCode.R)),
            item("Rescan Animations", MenuIcons.scan(), actions.rescanAnimations, key(KeyCode.R, shift = true)),
            SeparatorMenuItem(),
            item("Open .qodat Folder", MenuIcons.folder(), actions.openQodatFolder),
            SeparatorMenuItem(),
            item("Settings…", MenuIcons.settings(), actions.openSettings, key(KeyCode.COMMA)),
            SeparatorMenuItem(),
            item("Quit", MenuIcons.quit(), actions.quit, key(KeyCode.Q))
        )
    }

    private fun editMenu(actions: Actions) = Menu("_Edit").apply {
        isMnemonicParsing = true
        items.setAll(
            item("Undo", MenuIcons.undo(), actions.undo, key(KeyCode.Z)),
            item("Redo", MenuIcons.redo(), actions.redo, key(KeyCode.Z, shift = true)),
            SeparatorMenuItem(),
            item("Clear Scene", MenuIcons.clear(), actions.clearScene)
        )
    }

    private fun viewMenu(viewToggles: ViewToggles) = Menu("_View").apply {
        isMnemonicParsing = true
        items.setAll(
            check("Show Axis", viewToggles.showAxis),
            check("Show FPS", viewToggles.showFps),
            check("Show Unnamed Entities", viewToggles.showUnnamedEntities),
            check("Render Textures", viewToggles.renderTextures),
            CheckMenuItem("MSAA Antialiasing").apply {
                isSelected = viewToggles.antialiasing.get() == SceneAntialiasing.BALANCED
                selectedProperty().addListener { _, _, selected ->
                    viewToggles.antialiasing.set(
                        if (selected) SceneAntialiasing.BALANCED else SceneAntialiasing.DISABLED
                    )
                }
                viewToggles.antialiasing.addListener { _, _, value ->
                    val enabled = value == SceneAntialiasing.BALANCED
                    if (isSelected != enabled) isSelected = enabled
                }
            },
            SeparatorMenuItem(),
            check("Invert Camera", viewToggles.invertCamera)
        )
    }

    private fun helpMenu(actions: Actions) = Menu("_Help").apply {
        isMnemonicParsing = true
        items.setAll(
            item("Check for Updates…", MenuIcons.updates(), actions.checkForUpdates),
            item("About Qodat", MenuIcons.info(), actions.showAbout)
        )
    }

    private fun item(
        text: String,
        graphic: Region,
        action: () -> Unit,
        accelerator: KeyCombination? = null
    ) = MenuItem(text).apply {
        this.graphic = graphic
        accelerator?.let { this.accelerator = it }
        setOnAction { action() }
    }

    private fun check(text: String, property: BooleanProperty) =
        CheckMenuItem(text).apply {
            selectedProperty().setAndBind(property, biDirectional = true)
        }

    private fun key(code: KeyCode, shift: Boolean = false): KeyCombination =
        if (shift)
            KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
        else
            KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN)
}
