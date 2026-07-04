/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

enum class DbLockStartActivity {
  QUICK_UNLOCK,
  LAUNCHER
}

data class DbLockPlan(
  val notificationState: DbNotificationState,
  val startActivity: DbLockStartActivity?,
  val clearDb: Boolean,
  val finishNonHomeActivities: Boolean
)

object DbLockPlanner {

  fun plan(
    isAppForeground: Boolean,
    hasOpenDb: Boolean,
    isQuickUnlockEnabled: Boolean
  ): DbLockPlan {
    if (isQuickUnlockEnabled && hasOpenDb) {
      return DbLockPlan(
        notificationState = DbNotificationState.QUICK_UNLOCK,
        startActivity = if (isAppForeground) DbLockStartActivity.QUICK_UNLOCK else null,
        clearDb = false,
        finishNonHomeActivities = false
      )
    }

    return DbLockPlan(
      notificationState = DbNotificationState.LOCKED,
      startActivity = if (isAppForeground) DbLockStartActivity.LAUNCHER else null,
      clearDb = true,
      finishNonHomeActivities = isAppForeground && hasOpenDb
    )
  }
}
