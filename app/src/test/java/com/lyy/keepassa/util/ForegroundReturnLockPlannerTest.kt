/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundReturnLockPlannerTest {

  @Test fun nonHomeLockedPage_returnsLockOnly() {
    val plan = ForegroundReturnLockPlanner.plan(
      isHomeActivity = false,
      hasOpenDb = true,
      isLocked = true
    )

    assertEquals(ForegroundReturnLockAction.LOCK_ONLY, plan)
  }

  @Test fun nonHomeWithoutOpenDb_returnsLockOnly() {
    val plan = ForegroundReturnLockPlanner.plan(
      isHomeActivity = false,
      hasOpenDb = false,
      isLocked = false
    )

    assertEquals(ForegroundReturnLockAction.LOCK_ONLY, plan)
  }

  @Test fun homeLockedPage_doesNothing() {
    val plan = ForegroundReturnLockPlanner.plan(
      isHomeActivity = true,
      hasOpenDb = false,
      isLocked = true
    )

    assertEquals(ForegroundReturnLockAction.NONE, plan)
  }
}
