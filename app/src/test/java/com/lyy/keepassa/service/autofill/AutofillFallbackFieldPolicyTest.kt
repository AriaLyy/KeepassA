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
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillFallbackFieldPolicyTest {

  @Test fun browserFocusedVirtualNode_canAnchorAuthPromptEvenWithoutTextAutofillType() {
    assertTrue(
      AutofillFallbackFieldPolicy.canAnchorAuthPrompt(
        autofillType = View.AUTOFILL_TYPE_NONE,
        isAssistBlocked = false,
        isFocused = true,
        isAccessibilityFocused = false,
        isHtmlInput = false,
        className = "android.view.View",
        allowFocusedNonTextNodeFallback = true
      )
    )
  }

  @Test fun browserAccessibilityFocusedVirtualNode_canAnchorAuthPrompt() {
    assertTrue(
      AutofillFallbackFieldPolicy.canAnchorAuthPrompt(
        autofillType = View.AUTOFILL_TYPE_NONE,
        isAssistBlocked = false,
        isFocused = false,
        isAccessibilityFocused = true,
        isHtmlInput = false,
        className = "android.view.View",
        allowFocusedNonTextNodeFallback = true
      )
    )
  }

  @Test fun nativeFocusedNonTextNode_doesNotAnchorAuthPrompt() {
    assertFalse(
      AutofillFallbackFieldPolicy.canAnchorAuthPrompt(
        autofillType = View.AUTOFILL_TYPE_NONE,
        isAssistBlocked = false,
        isFocused = true,
        isAccessibilityFocused = false,
        isHtmlInput = false,
        className = "android.view.View",
        allowFocusedNonTextNodeFallback = false
      )
    )
  }

  @Test fun assistBlockedNode_doesNotAnchorAuthPrompt() {
    assertFalse(
      AutofillFallbackFieldPolicy.canAnchorAuthPrompt(
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isAssistBlocked = true,
        isFocused = true,
        isAccessibilityFocused = false,
        isHtmlInput = true,
        className = "android.widget.EditText",
        allowFocusedNonTextNodeFallback = true
      )
    )
  }

  @Test fun browserRequestFocusedId_canAnchorAuthPromptWhenParserMissesWebField() {
    assertTrue(
      AutofillFallbackFieldPolicy.canUseRequestFocusedId(
        hasRequestFocusedId = true,
        requestFocusedIdIsSearchOrUrlField = false,
        strategyAllowsRequestFocusedIdFallback = true
      )
    )
  }

  @Test fun requestFocusedSearchOrUrlField_doesNotAnchorAuthPrompt() {
    assertFalse(
      AutofillFallbackFieldPolicy.canUseRequestFocusedId(
        hasRequestFocusedId = true,
        requestFocusedIdIsSearchOrUrlField = true,
        strategyAllowsRequestFocusedIdFallback = true,
        strategyAllowsSearchOrUrlRequestFocusedIdFallback = false
      )
    )
  }

  @Test fun miBrowserRequestFocusedSearchOrUrlField_canAnchorAuthPromptWhenStrategyAllowsIt() {
    assertTrue(
      AutofillFallbackFieldPolicy.canUseRequestFocusedId(
        hasRequestFocusedId = true,
        requestFocusedIdIsSearchOrUrlField = true,
        strategyAllowsRequestFocusedIdFallback = true,
        strategyAllowsSearchOrUrlRequestFocusedIdFallback = true
      )
    )
  }

  @Test fun chromiumStrategyDisallowsRequestFocusedIdFallback() {
    assertFalse(
      AutofillFallbackFieldPolicy.canUseRequestFocusedId(
        hasRequestFocusedId = true,
        requestFocusedIdIsSearchOrUrlField = false,
        strategyAllowsRequestFocusedIdFallback = false
      )
    )
  }

  @Test fun defaultFallbackAuthPromptAnchorPrefersParserFallbackId() {
    val parserFallbackId = mockk<AutofillId>()
    val requestFocusedId = mockk<AutofillId>()

    val fallbackId = AutofillFallbackFieldPolicy.resolveFallbackAuthPromptId(
      parserFallbackId = parserFallbackId,
      requestFocusedId = requestFocusedId,
      requestFocusedIdIsSearchOrUrlField = false,
      strategyAllowsRequestFocusedIdFallback = true,
      strategyAllowsSearchOrUrlRequestFocusedIdFallback = false,
      strategyPrefersRequestFocusedIdForAuthPrompt = false
    )

    assertSame(parserFallbackId, fallbackId)
  }

  @Test fun miBrowserFallbackAuthPromptAnchorPrefersCurrentWebFieldId() {
    val parserFallbackId = mockk<AutofillId>()
    val requestFocusedId = mockk<AutofillId>()

    val fallbackId = AutofillFallbackFieldPolicy.resolveFallbackAuthPromptId(
      parserFallbackId = parserFallbackId,
      requestFocusedId = requestFocusedId,
      requestFocusedIdIsSearchOrUrlField = false,
      strategyAllowsRequestFocusedIdFallback = true,
      strategyAllowsSearchOrUrlRequestFocusedIdFallback = false,
      strategyPrefersRequestFocusedIdForAuthPrompt = true
    )

    assertSame(requestFocusedId, fallbackId)
  }

  @Test fun miBrowserSearchOrUrlFocusedSessionStillAnchorsAuthPrompt() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.mi.globalbrowser")
    val parserFallbackId = mockk<AutofillId>()
    val requestFocusedId = mockk<AutofillId>()

    val fallbackId = AutofillFallbackFieldPolicy.resolveFallbackAuthPromptId(
      parserFallbackId = parserFallbackId,
      requestFocusedId = requestFocusedId,
      requestFocusedIdIsSearchOrUrlField = true,
      strategyAllowsRequestFocusedIdFallback = strategy.allowRequestFocusedIdFallback,
      strategyAllowsSearchOrUrlRequestFocusedIdFallback = strategy.allowSearchOrUrlRequestFocusedIdFallback,
      strategyPrefersRequestFocusedIdForAuthPrompt = strategy.preferRequestFocusedIdForAuthPromptFallback
    )

    assertNotNull("MI Browser 上聚焦到 URL/搜索栏时也必须产生 fallback id 用于弹出认证 UI", fallbackId)
  }
}
