/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId

internal data class AutofillSingleFieldFallbackTarget(
  val fallbackId: AutofillId,
  val fallbackRole: BrowserFormFieldRole?,
  val domain: String?
)

internal object AutofillSingleFieldFallbackPolicy {

  fun resolve(
    strategy: BrowserAutofillStrategy,
    currentFallbackId: AutofillId?,
    currentFallbackRole: BrowserFormFieldRole?,
    currentDomain: String?,
    storedContext: AutofillBrowserAuthContext?
  ): AutofillSingleFieldFallbackTarget? {
    if (!strategy.allowSingleFieldAuthFallback) {
      return null
    }

    val normalizedCurrentDomain = currentDomain?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedStoredDomain = storedContext?.domain?.trim()?.takeIf { it.isNotEmpty() }

    currentFallbackId?.let {
      return AutofillSingleFieldFallbackTarget(
        fallbackId = it,
        fallbackRole = currentFallbackRole,
        domain = normalizedCurrentDomain
      )
    }

    val storedFallbackId = storedContext?.fallbackId ?: return null
    return AutofillSingleFieldFallbackTarget(
      fallbackId = storedFallbackId,
      fallbackRole = storedContext.fallbackRole,
      domain = normalizedCurrentDomain ?: normalizedStoredDomain
    )
  }
}
