package stan.qodat.scene.control.dialog

import javafx.scene.control.ButtonType
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheChooserDialogTest {

    @Test
    fun okReturnsBothDirectories() {
        val root = Path.of("/tmp/qodat")
        val cache = Path.of("/tmp/qodat/caches/OS")
        assertEquals(root to cache, CacheChooserDialog.resultOf(ButtonType.OK, root, cache))
    }

    @Test
    fun cancelCloseAndMissingPathsDoNotThrow() {
        val root = Path.of("/tmp/qodat")
        val cache = Path.of("/tmp/qodat/caches/OS")
        assertNull(CacheChooserDialog.resultOf(ButtonType.CANCEL, root, cache))
        assertNull(CacheChooserDialog.resultOf(ButtonType.CLOSE, root, cache))
        assertNull(CacheChooserDialog.resultOf(null, root, cache))
        assertNull(CacheChooserDialog.resultOf(ButtonType.OK, null, cache))
        assertNull(CacheChooserDialog.resultOf(ButtonType.OK, root, null))
    }
}
