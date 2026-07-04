/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbLockPlannerTest {

  @Test fun backgroundWithQuickUnlock_onlyShowsQuickUnlockNotification() {
    val plan = DbLockPlanner.plan(
      isAppForeground = false,
      hasOpenDb = true,
      isQuickUnlockEnabled = true
    )

    assertEquals(DbNotificationState.QUICK_UNLOCK, plan.notificationState)
    assertEquals(null, plan.startActivity)
    assertFalse(plan.clearDb)
    assertFalse(plan.finishNonHomeActivities)
  }

  @Test fun backgroundWithoutQuickUnlock_onlyShowsLockedNotificationAndClearsDb() {
    val plan = DbLockPlanner.plan(
      isAppForeground = false,
      hasOpenDb = true,
      isQuickUnlockEnabled = false
    )

    assertEquals(DbNotificationState.LOCKED, plan.notificationState)
    assertEquals(null, plan.startActivity)
    assertTrue(plan.clearDb)
    assertFalse(plan.finishNonHomeActivities)
  }

  @Test fun foregroundWithQuickUnlock_startsQuickUnlockActivity() {
    val plan = DbLockPlanner.plan(
      isAppForeground = true,
      hasOpenDb = true,
      isQuickUnlockEnabled = true
    )

    assertEquals(DbNotificationState.QUICK_UNLOCK, plan.notificationState)
    assertEquals(DbLockStartActivity.QUICK_UNLOCK, plan.startActivity)
    assertFalse(plan.clearDb)
    assertFalse(plan.finishNonHomeActivities)
  }

  @Test fun quickUnlockEnabledWithoutOpenDb_fallsBackToLockedNotification() {
    val plan = DbLockPlanner.plan(
      isAppForeground = false,
      hasOpenDb = false,
      isQuickUnlockEnabled = true
    )

    assertEquals(DbNotificationState.LOCKED, plan.notificationState)
    assertEquals(null, plan.startActivity)
    assertTrue(plan.clearDb)
    assertFalse(plan.finishNonHomeActivities)
  }

  @Test fun foregroundWithoutOpenDb_startsLauncherLockPageWithoutFinishingStack() {
    val plan = DbLockPlanner.plan(
      isAppForeground = true,
      hasOpenDb = false,
      isQuickUnlockEnabled = false
    )

    assertEquals(DbNotificationState.LOCKED, plan.notificationState)
    assertEquals(DbLockStartActivity.LAUNCHER, plan.startActivity)
    assertTrue(plan.clearDb)
    assertFalse(plan.finishNonHomeActivities)
  }

  @Test fun foregroundWithoutQuickUnlock_startsLauncherAndClearsDb() {
    val plan = DbLockPlanner.plan(
      isAppForeground = true,
      hasOpenDb = true,
      isQuickUnlockEnabled = false
    )

    assertEquals(DbNotificationState.LOCKED, plan.notificationState)
    assertEquals(DbLockStartActivity.LAUNCHER, plan.startActivity)
    assertTrue(plan.clearDb)
    assertTrue(plan.finishNonHomeActivities)
  }
}
