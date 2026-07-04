/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

enum class ForegroundReturnLockAction {
  NONE,
  LOCK_ONLY
}

object ForegroundReturnLockPlanner {

  fun plan(
    isHomeActivity: Boolean,
    hasOpenDb: Boolean,
    isLocked: Boolean
  ): ForegroundReturnLockAction {
    if (isHomeActivity) {
      return ForegroundReturnLockAction.NONE
    }
    return if (!hasOpenDb || isLocked) {
      ForegroundReturnLockAction.LOCK_ONLY
    } else {
      ForegroundReturnLockAction.NONE
    }
  }
}
