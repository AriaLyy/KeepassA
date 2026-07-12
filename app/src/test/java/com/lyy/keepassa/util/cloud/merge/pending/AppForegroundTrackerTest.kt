package com.lyy.keepassa.util.cloud.merge.pending

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppForegroundTrackerTest {

  @Test
  fun appIsForegroundWhileAtLeastOneActivityIsStarted() {
    val tracker = AppForegroundTracker()

    assertFalse(tracker.isForeground)
    tracker.onActivityStarted()
    tracker.onActivityStarted()
    assertTrue(tracker.isForeground)

    tracker.onActivityStopped()
    assertTrue(tracker.isForeground)
    tracker.onActivityStopped()
    assertFalse(tracker.isForeground)
  }

  @Test
  fun extraStopDoesNotMakeFutureForegroundTrackingInvalid() {
    val tracker = AppForegroundTracker()

    tracker.onActivityStopped()
    tracker.onActivityStarted()

    assertTrue(tracker.isForeground)
  }
}
