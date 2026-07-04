/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

interface AutoLockLastStartTimeStorage {
  var lastStartTimeMs: Long
}

class AutoLockTimerGate(
  private val storage: AutoLockLastStartTimeStorage,
  private val clock: () -> Long = System::currentTimeMillis,
  private val throttleMs: Long = THROTTLE_MS
) {

  fun tryAcquire(): Boolean {
    val now = clock()
    val lastStartTime = storage.lastStartTimeMs
    if (lastStartTime > 0 && now - lastStartTime <= throttleMs) {
      return false
    }
    storage.lastStartTimeMs = now
    return true
  }

  companion object {
    const val THROTTLE_MS = 3000L
  }
}

object CommonKvAutoLockLastStartTimeStorage : AutoLockLastStartTimeStorage {
  private const val KEY_LAST_START_TIME = "LastStartTime"

  override var lastStartTimeMs: Long
    get() = CommonKVStorage.getLong(KEY_LAST_START_TIME, -1L)
    set(value) {
      CommonKVStorage.put(KEY_LAST_START_TIME, value)
    }
}
