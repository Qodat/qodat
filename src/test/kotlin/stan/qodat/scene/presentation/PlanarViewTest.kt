package stan.qodat.scene.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanarViewTest {

    @Test
    fun cameraStaysLockedIn2DWhenActive() {
        val previousActive = PlanarView.active.get()
        val previousExploded = PlanarView.exploded.get()
        try {
            PlanarView.active.set(true)
            PlanarView.exploded.set(false)
            assertFalse(PlanarView.cameraNavigationEnabled.get())

            PlanarView.exploded.set(true)
            assertTrue(PlanarView.cameraNavigationEnabled.get())

            PlanarView.active.set(false)
            PlanarView.exploded.set(false)
            assertTrue(PlanarView.cameraNavigationEnabled.get())
        } finally {
            PlanarView.active.set(previousActive)
            PlanarView.exploded.set(previousExploded)
        }
    }
}
