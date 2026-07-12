package com.lyy.keepassa.util.cloud.merge.pending

enum class PendingMergeNotificationTarget {
  QUICK_UNLOCK,
  FULL_UNLOCK
}

object PendingMergeNotificationTargetPolicy {
  fun choose(
    hasOpenDatabase: Boolean,
    canQuickUnlock: Boolean
  ): PendingMergeNotificationTarget {
    return if (hasOpenDatabase && canQuickUnlock) {
      PendingMergeNotificationTarget.QUICK_UNLOCK
    } else {
      PendingMergeNotificationTarget.FULL_UNLOCK
    }
  }
}
