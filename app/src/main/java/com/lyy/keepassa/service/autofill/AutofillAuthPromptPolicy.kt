/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

internal object AutofillAuthPromptPolicy {

  fun shouldUseFallbackAuthPrompt(
    needAuth: Boolean,
    classifiedFieldCount: Int,
    hasFallbackFillId: Boolean,
    isWebContext: Boolean
  ): Boolean {
    return needAuth && classifiedFieldCount == 0 && hasFallbackFillId && isWebContext
  }
}
