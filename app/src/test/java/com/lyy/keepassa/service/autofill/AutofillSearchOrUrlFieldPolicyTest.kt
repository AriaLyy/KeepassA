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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSearchOrUrlFieldPolicyTest {

  @Test fun miBrowserIgnoresClassifiedFieldsWhenOnlySearchOrUrlFieldsWereFound() {
    val urlFieldId = mockk<AutofillId>()

    assertTrue(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        strategy = BrowserAutofillStrategyRegistry.forPackage("com.mi.globalbrowser"),
        classifiedIds = setOf(urlFieldId),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }

  @Test fun miBrowserKeepsClassifiedFieldsWhenAWebCredentialFieldWasFound() {
    val urlFieldId = mockk<AutofillId>()
    val webFieldId = mockk<AutofillId>()

    assertFalse(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        strategy = BrowserAutofillStrategyRegistry.forPackage("com.mi.globalbrowser"),
        classifiedIds = setOf(urlFieldId, webFieldId),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }

  @Test fun edgeDoesNotIgnoreSearchOrUrlOnlyFieldsByMiBrowserPolicy() {
    val urlFieldId = mockk<AutofillId>()

    assertFalse(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        strategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx"),
        classifiedIds = setOf(urlFieldId),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }
}
