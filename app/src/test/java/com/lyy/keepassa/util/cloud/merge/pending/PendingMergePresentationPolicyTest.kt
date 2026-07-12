package com.lyy.keepassa.util.cloud.merge.pending

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingMergePresentationPolicyTest {

  @Test
  fun foregroundUnlockedDatabaseOpensConflictActivity() {
    assertEquals(
      PendingMergePresentation.OPEN_ACTIVITY,
      PendingMergePresentationPolicy.decide(
        isAppForeground = true,
        isDatabaseUnlocked = true,
        canPostNotifications = false
      )
    )
  }

  @Test
  fun backgroundWithNotificationPermissionPostsNotification() {
    assertEquals(
      PendingMergePresentation.POST_NOTIFICATION,
      PendingMergePresentationPolicy.decide(
        isAppForeground = false,
        isDatabaseUnlocked = true,
        canPostNotifications = true
      )
    )
  }

  @Test
  fun backgroundWithoutNotificationPermissionWaitsForUnlockResume() {
    assertEquals(
      PendingMergePresentation.WAIT_FOR_UNLOCK,
      PendingMergePresentationPolicy.decide(
        isAppForeground = false,
        isDatabaseUnlocked = true,
        canPostNotifications = false
      )
    )
  }
}
