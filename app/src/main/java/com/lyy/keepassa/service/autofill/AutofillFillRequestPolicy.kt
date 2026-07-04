/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

internal object AutofillFillRequestPolicy {

  fun shouldCompleteWithNullResponse(
    targetPackageName: String,
    servicePackageName: String,
    canBackgroundStart: Boolean
  ): Boolean {
    return targetPackageName.equals(servicePackageName, ignoreCase = true) || !canBackgroundStart
  }
}
