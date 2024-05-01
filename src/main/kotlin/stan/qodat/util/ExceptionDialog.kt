package stan.qodat.util

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import java.io.PrintWriter
import java.io.StringWriter


fun<T> runCatchingWithDialog(
    activityName: String,
    block: () -> T
) : Result<T> {
    return runCatching {
        block()
    }.onFailure {

        val alert = Alert(AlertType.ERROR)
        alert.title = "Exception Dialog"
        alert.headerText = "Something went wrong in $activityName"
        alert.contentText = it.localizedMessage

        // Create expandable Exception.
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        it.printStackTrace(pw)
        val exceptionText = sw.toString()

        val label: Label = Label("The exception stacktrace was:")

        val textArea: TextArea = TextArea(exceptionText)
        textArea.isEditable = false
        textArea.isWrapText = true

        textArea.maxWidth = Double.MAX_VALUE
        textArea.maxHeight = Double.MAX_VALUE
        GridPane.setVgrow(textArea, Priority.ALWAYS)
        GridPane.setHgrow(textArea, Priority.ALWAYS)

        val expContent = GridPane()
        expContent.maxWidth = Double.MAX_VALUE
        expContent.add(label, 0, 0)
        expContent.add(textArea, 0, 1)


        // Set expandable Exception into the dialog pane.
        alert.dialogPane.expandableContent = expContent
        alert.showAndWait()
    }
}
