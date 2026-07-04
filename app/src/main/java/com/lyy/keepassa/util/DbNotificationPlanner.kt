/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

enum class DbNotificationState {
  UNLOCKED,
  LOCKED,
  QUICK_UNLOCK
}

data class DbNotificationPlan(
  val notifyId: Int,
  val cancelIds: Set<Int>,
  val ongoing: Boolean
)

object DbNotificationPlanner {
  const val DB_STATUS_ID = 10001
  const val LEGACY_QUICK_UNLOCK_ID = 10002

  fun plan(state: DbNotificationState): DbNotificationPlan {
    return DbNotificationPlan(
      notifyId = DB_STATUS_ID,
      cancelIds = setOf(LEGACY_QUICK_UNLOCK_ID),
      ongoing = true
    )
  }
}
