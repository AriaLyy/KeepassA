package com.lyy.keepassa.util.cloud.merge.pending

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingMergeNotificationTargetPolicyTest {

  @Test
  fun openDatabaseWithQuickUnlockUsesQuickUnlockTarget() {
    assertEquals(
      PendingMergeNotificationTarget.QUICK_UNLOCK,
      PendingMergeNotificationTargetPolicy.choose(hasOpenDatabase = true, canQuickUnlock = true)
    )
  }

  @Test
  fun missingDatabaseOrQuickUnlockUsesFullUnlockTarget() {
    assertEquals(
      PendingMergeNotificationTarget.FULL_UNLOCK,
      PendingMergeNotificationTargetPolicy.choose(hasOpenDatabase = false, canQuickUnlock = true)
    )
    assertEquals(
      PendingMergeNotificationTarget.FULL_UNLOCK,
      PendingMergeNotificationTargetPolicy.choose(hasOpenDatabase = true, canQuickUnlock = false)
    )
  }
}
