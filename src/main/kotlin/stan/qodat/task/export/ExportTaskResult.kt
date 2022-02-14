package stan.qodat.task.export

import java.nio.file.Path

sealed class ExportTaskResult {
    class Success(val saveDir: Path) : ExportTaskResult()
    class Failed(val exception: Exception) : ExportTaskResult()
}