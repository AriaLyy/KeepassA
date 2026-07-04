/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillAuthPromptPolicyTest {

  @Test fun lockedBrowserRequest_withoutClassifiedFields_usesFallbackFieldForAuthPrompt() {
    assertTrue(
      AutofillAuthPromptPolicy.shouldUseFallbackAuthPrompt(
        needAuth = true,
        classifiedFieldCount = 0,
        hasFallbackFillId = true,
        isWebContext = true
      )
    )
  }

  @Test fun unlockedRequest_doesNotUseFallbackAuthPrompt() {
    assertFalse(
      AutofillAuthPromptPolicy.shouldUseFallbackAuthPrompt(
        needAuth = false,
        classifiedFieldCount = 0,
        hasFallbackFillId = true,
        isWebContext = true
      )
    )
  }

  @Test fun lockedRequestWithClassifiedFields_usesNormalAuthResponse() {
    assertFalse(
      AutofillAuthPromptPolicy.shouldUseFallbackAuthPrompt(
        needAuth = true,
        classifiedFieldCount = 1,
        hasFallbackFillId = true,
        isWebContext = true
      )
    )
  }

  @Test fun lockedBrowserRequestWithoutFallbackField_returnsNoAuthPrompt() {
    assertFalse(
      AutofillAuthPromptPolicy.shouldUseFallbackAuthPrompt(
        needAuth = true,
        classifiedFieldCount = 0,
        hasFallbackFillId = false,
        isWebContext = true
      )
    )
  }

  @Test fun lockedNativeRequestWithoutClassifiedFields_doesNotPromptFromGenericTextField() {
    assertFalse(
      AutofillAuthPromptPolicy.shouldUseFallbackAuthPrompt(
        needAuth = true,
        classifiedFieldCount = 0,
        hasFallbackFillId = true,
        isWebContext = false
      )
    )
  }
}
