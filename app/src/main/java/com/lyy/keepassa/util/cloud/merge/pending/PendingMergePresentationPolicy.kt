package com.lyy.keepassa.util.cloud.merge.pending

enum class PendingMergePresentation {
  OPEN_ACTIVITY,
  POST_NOTIFICATION,
  WAIT_FOR_UNLOCK
}

object PendingMergePresentationPolicy {
  fun decide(
    isAppForeground: Boolean,
    isDatabaseUnlocked: Boolean,
    canPostNotifications: Boolean
  ): PendingMergePresentation {
    if (isAppForeground && isDatabaseUnlocked) {
      return PendingMergePresentation.OPEN_ACTIVITY
    }
    if (!isAppForeground && canPostNotifications) {
      return PendingMergePresentation.POST_NOTIFICATION
    }
    return PendingMergePresentation.WAIT_FOR_UNLOCK
  }
}
