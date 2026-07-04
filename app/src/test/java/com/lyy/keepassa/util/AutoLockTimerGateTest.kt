/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockTimerGateTest {

  @Test fun firstUserActivity_allowsTimerResetAndStoresNow() {
    val storage = FakeLastStartTimeStorage()
    val gate = AutoLockTimerGate(storage, clock = { 10_000L })

    assertTrue(gate.tryAcquire())
    assertTrue(storage.lastStartTimeMs == 10_000L)
  }

  @Test fun userActivityWithinThrottle_returnsFalseAndKeepsPreviousTimestamp() {
    val storage = FakeLastStartTimeStorage(lastStartTimeMs = 10_000L)
    val gate = AutoLockTimerGate(storage, clock = { 12_999L })

    assertFalse(gate.tryAcquire())
    assertTrue(storage.lastStartTimeMs == 10_000L)
  }

  @Test fun userActivityAfterThrottle_allowsTimerResetAndUpdatesTimestamp() {
    val storage = FakeLastStartTimeStorage(lastStartTimeMs = 10_000L)
    val gate = AutoLockTimerGate(storage, clock = { 13_001L })

    assertTrue(gate.tryAcquire())
    assertTrue(storage.lastStartTimeMs == 13_001L)
  }

  private class FakeLastStartTimeStorage(
    override var lastStartTimeMs: Long = -1L
  ) : AutoLockLastStartTimeStorage
}
