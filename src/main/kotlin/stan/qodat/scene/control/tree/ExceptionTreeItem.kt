package stan.qodat.scene.control.tree

import javafx.scene.control.TreeItem
import javafx.scene.text.TextFlow
import stan.qodat.javafx.menloText
import stan.qodat.javafx.treeItem
import stan.qodat.scene.controller.EventLogController
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.LOG_ERROR
import stan.qodat.util.LOG_VERBOSE
import stan.qodat.util.PURPLE

/**
 * Represents a [TreeItem] for the provided [titledThrowable].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class ExceptionTreeItem(
    private val titledThrowable: EventLogController.TitledThrowable
) : TreeItem<TextFlow>() {

    init {
        val date = titledThrowable.date
        val title = titledThrowable.title
        val throwable = titledThrowable.throwable
        value = TextFlow().apply {
            menloText("${date.hour.addZeroIfBelow10()}" +
                    ":${date.minute.addZeroIfBelow10()}" +
                    ":${date.second.addZeroIfBelow10()}  " to PURPLE)
            menloText(title to BABY_BLUE)
            menloText(" -> " to PURPLE)
            menloText("${throwable::class.simpleName}: ${throwable.message}" to LOG_ERROR)
        }
        addToTree(throwable.stackTrace.iterator())
    }
    private fun Int.addZeroIfBelow10() = let { if (it < 10) "0$it" else it}

    private fun TreeItem<TextFlow>.addToTree(stackTrace: Iterator<StackTraceElement>, lastClassName: String? = null) {
        if (stackTrace.hasNext()) {
            var next : StackTraceElement
            do {
                next = stackTrace.next()
                treeItem {
                    isExpanded = false
                    value = TextFlow().apply {
                        menloText(
                            "${next.className}.${next.methodName}(" to LOG_ERROR,
                            fileName(next) to LOG_VERBOSE,
                            ":${next.lineNumber}" to LOG_VERBOSE,
                            ")" to LOG_ERROR,
                        )
                    }
                    if (stackTrace.hasNext()) {
                        if (next.className != lastClassName) {
                            addToTree(stackTrace, next.className)
                            return@treeItem
                        }
                    }
                }
            } while (stackTrace.hasNext())
        }
    }

    private fun fileName(next: StackTraceElement) = next.fileName?:"???"
}