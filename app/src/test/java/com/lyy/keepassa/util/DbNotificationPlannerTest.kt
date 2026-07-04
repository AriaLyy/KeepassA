/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：快速解锁锁定状态不能留下旧的“已解锁”通知。
 */
class DbNotificationPlannerTest {

  @Test fun allDbStates_useSingleStatusNotificationId() {
    val notifyIds = DbNotificationState.entries.map {
      DbNotificationPlanner.plan(it).notifyId
    }.toSet()

    assertEquals(setOf(DbNotificationPlanner.DB_STATUS_ID), notifyIds)
  }

  @Test fun quickUnlock_cancelsLegacyQuickUnlockNotificationId() {
    val plan = DbNotificationPlanner.plan(DbNotificationState.QUICK_UNLOCK)

    assertEquals(DbNotificationPlanner.DB_STATUS_ID, plan.notifyId)
    assertTrue(plan.cancelIds.contains(DbNotificationPlanner.LEGACY_QUICK_UNLOCK_ID))
  }

  @Test fun dbStatusNotification_isOngoing() {
    DbNotificationState.entries.forEach {
      assertTrue(DbNotificationPlanner.plan(it).ongoing)
    }
  }
}
