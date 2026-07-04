/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillSingleFieldFallbackPolicyTest {

  @Test fun yandexStrategyCanUseCurrentFocusedIdWithoutStoredContext() {
    val fallbackId = mockk<AutofillId>()

    val target = AutofillSingleFieldFallbackPolicy.resolve(
      strategy = BrowserAutofillStrategyRegistry.forPackage("com.yandex.browser"),
      currentFallbackId = fallbackId,
      currentFallbackRole = null,
      currentDomain = null,
      storedContext = null
    )

    assertSame(fallbackId, target?.fallbackId)
    assertNull(target?.fallbackRole)
    assertNull(target?.domain)
  }

  @Test fun conservativeBrowserDoesNotUseCurrentFocusedId() {
    val fallbackId = mockk<AutofillId>()

    val target = AutofillSingleFieldFallbackPolicy.resolve(
      strategy = BrowserAutofillStrategyRegistry.forPackage("com.mx.browser"),
      currentFallbackId = fallbackId,
      currentFallbackRole = null,
      currentDomain = null,
      storedContext = null
    )

    assertNull(target)
  }

  @Test fun currentFocusedIdDoesNotInheritStoredRoleOrDomain() {
    val currentFallbackId = mockk<AutofillId>()
    val storedFallbackId = mockk<AutofillId>()
    val storedContext = AutofillBrowserAuthContext(
      packageName = "com.yandex.browser",
      domain = "example.com",
      metadata = null,
      fallbackId = storedFallbackId,
      fallbackRole = BrowserFormFieldRole.PASSWORD,
      createdAtMs = 1_000
    )

    val target = AutofillSingleFieldFallbackPolicy.resolve(
      strategy = BrowserAutofillStrategyRegistry.forPackage("com.yandex.browser"),
      currentFallbackId = currentFallbackId,
      currentFallbackRole = null,
      currentDomain = null,
      storedContext = storedContext
    )

    assertSame(currentFallbackId, target?.fallbackId)
    assertNull(target?.fallbackRole)
    assertNull(target?.domain)
  }
}
