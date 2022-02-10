package stan.qodat.javafx

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.stage.Stage
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.IOException


object JavaFXTrayIcon {

    private lateinit var trayIcon: TrayIcon

    fun close(){
        if (this::trayIcon.isInitialized)
            SystemTray.getSystemTray().remove(trayIcon)
    }

    /**
     * Sets up a system tray icon for the application.
     */
    fun addAppToTray(stage: Stage, icon: javafx.scene.image.Image) {
        try {
            // ensure awt toolkit is initialized.
            Toolkit.getDefaultToolkit()

            // app requires system tray support, just exit if there is no support.
            if (!SystemTray.isSupported()) {
                println("No system tray support, application exiting.")
                return
            }

            // set up a system tray icon.
            val tray = SystemTray.getSystemTray()

            val trayIcon = TrayIcon(SwingFXUtils.fromFXImage(icon, null))

            trayIcon.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    Platform.runLater { show(stage) }
                }
            })

            tray.add(trayIcon)
        } catch (e: AWTException) {
            println("Unable to init system tray")
            e.printStackTrace()
        } catch (e: IOException) {
            println("Unable to init system tray")
            e.printStackTrace()
        }
    }

    private fun show(stage: Stage) {
        stage.show()
        stage.toFront()
    }
}