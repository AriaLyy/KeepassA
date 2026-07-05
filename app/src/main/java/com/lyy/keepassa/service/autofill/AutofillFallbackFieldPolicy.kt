/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.View
import android.view.autofill.AutofillId

internal object AutofillFallbackFieldPolicy {

  fun canAnchorAuthPrompt(
    autofillType: Int,
    isAssistBlocked: Boolean,
    isFocused: Boolean,
    isAccessibilityFocused: Boolean,
    isHtmlInput: Boolean,
    className: String?,
    allowFocusedNonTextNodeFallback: Boolean
  ): Boolean {
    if (isAssistBlocked) {
      return false
    }

    val hasFocus = isFocused || isAccessibilityFocused
    if (allowFocusedNonTextNodeFallback && hasFocus) {
      return true
    }

    if (autofillType != View.AUTOFILL_TYPE_TEXT) {
      return false
    }

    return hasFocus || isHtmlInput || AutofillViewClassPolicy.isEditTextClassName(className)
  }

  fun canUseRequestFocusedId(
    hasRequestFocusedId: Boolean,
    requestFocusedIdIsSearchOrUrlField: Boolean,
    strategyAllowsRequestFocusedIdFallback: Boolean,
    strategyAllowsSearchOrUrlRequestFocusedIdFallback: Boolean = false
  ): Boolean {
    return strategyAllowsRequestFocusedIdFallback &&
      hasRequestFocusedId &&
      (!requestFocusedIdIsSearchOrUrlField || strategyAllowsSearchOrUrlRequestFocusedIdFallback)
  }

  fun resolveFallbackAuthPromptId(
    parserFallbackId: AutofillId?,
    requestFocusedId: AutofillId?,
    requestFocusedIdIsSearchOrUrlField: Boolean,
    strategyAllowsRequestFocusedIdFallback: Boolean,
    strategyAllowsSearchOrUrlRequestFocusedIdFallback: Boolean,
    strategyPrefersRequestFocusedIdForAuthPrompt: Boolean
  ): AutofillId? {
    val usableRequestFocusedId = requestFocusedId?.takeIf {
      canUseRequestFocusedId(
        hasRequestFocusedId = true,
        requestFocusedIdIsSearchOrUrlField = requestFocusedIdIsSearchOrUrlField,
        strategyAllowsRequestFocusedIdFallback = strategyAllowsRequestFocusedIdFallback,
        strategyAllowsSearchOrUrlRequestFocusedIdFallback =
          strategyAllowsSearchOrUrlRequestFocusedIdFallback
      )
    }

    if (
      strategyPrefersRequestFocusedIdForAuthPrompt &&
      requestFocusedId != null &&
      requestFocusedIdIsSearchOrUrlField &&
      !strategyAllowsSearchOrUrlRequestFocusedIdFallback
    ) {
      return null
    }

    return if (strategyPrefersRequestFocusedIdForAuthPrompt) {
      usableRequestFocusedId ?: parserFallbackId
    } else {
      parserFallbackId ?: usableRequestFocusedId
    }
  }
}
