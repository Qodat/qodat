package stan.qodat.task.export

import java.nio.file.Path

/**
 * The result returned by an [ExportTask].
 */
sealed class ExportTaskResult {

    /**
     * The [ExportTask] successfully exported some object to a file (or multiple files) in [saveDir].
     */
    class Success(val saveDir: Path) : ExportTaskResult()

    /**
     * The [ExportTask] failed and threw [exception].
     */
    class Failed(val exception: Exception) : ExportTaskResult()
}