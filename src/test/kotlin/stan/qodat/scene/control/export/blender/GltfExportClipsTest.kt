package stan.qodat.scene.control.export.blender

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GltfExportClipsTest {

    @Test
    fun exportRolesAreIdleAndWalkIncludingCompactLabels() {
        assertTrue(GltfExportClips.isExportRole("Idle"))
        assertTrue(GltfExportClips.isExportRole("Walk"))
        assertTrue(GltfExportClips.isExportRole("Walk · Run"))
        assertTrue(GltfExportClips.isExportRole("Idle · Walk"))
        assertFalse(GltfExportClips.isExportRole("Idle rotate left"))
        assertFalse(GltfExportClips.isExportRole("Rotate 180"))
        assertFalse(GltfExportClips.isExportRole("Run"))
        assertFalse(GltfExportClips.isExportRole("Crawl"))
    }
}
