package stan.qodat.desktop

import dev.hydraulic.conveyor.control.SoftwareUpdateController

/**
 * Thin wrapper around Conveyor's Control API (`SoftwareUpdateController`).
 *
 * [getInstance][SoftwareUpdateController.getInstance] is null unless this
 * process was started by the Conveyor native launcher. Gradle / IDE launches
 * cannot see the Sparkle or Windows update engines.
 */
object SoftwareUpdates {

    sealed class Result {
        data class Available(val current: String, val latest: String) : Result()
        data class UpToDate(val current: String) : Result()
        data object NotPackaged : Result()
        data class Unavailable(val message: String) : Result()
        data class Failed(val message: String) : Result()
    }

    fun controller(): SoftwareUpdateController? = SoftwareUpdateController.getInstance()

    /**
     * Compare the packaged version to `metadata.properties` on
     * `app.repositoryUrl`. Must not run on the JavaFX thread — this hits the
     * network.
     */
    fun check(controller: SoftwareUpdateController? = controller()): Result {
        if (controller == null) return Result.NotPackaged
        val current = controller.currentVersion
            ?: return Result.Unavailable("This package did not report an installed version.")
        return try {
            val latest = controller.currentVersionFromRepository
                ?: return Result.Unavailable("The update site did not report a latest version.")
            if (latest > current) {
                Result.Available(current.toString(), latest.toString())
            } else {
                Result.UpToDate(current.toString())
            }
        } catch (e: SoftwareUpdateController.UpdateCheckException) {
            Result.Failed(e.message?.ifBlank { null } ?: "Could not reach the update site.")
        }
    }

    /**
     * Ask Sparkle / updatecheck.exe to download and apply. On Mac and Windows
     * this can restart the process — save work first. Only call after [check]
     * returned [Result.Available].
     */
    fun trigger(controller: SoftwareUpdateController? = controller()) {
        val instance = controller ?: error("Not running inside a Conveyor package.")
        when (val availability = instance.canTriggerUpdateCheckUI()) {
            SoftwareUpdateController.Availability.AVAILABLE -> instance.triggerUpdateCheckUI()
            SoftwareUpdateController.Availability.UNIMPLEMENTED ->
                error("This platform does not support in-app updates (use the system package manager).")
            else -> error("Update UI is not available ($availability).")
        }
    }
}
