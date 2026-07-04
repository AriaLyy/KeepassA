/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillEntryLookupPolicyTest {

  @Test fun browserWithDomainLooksUpDomainOnly() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx")

    assertEquals(
      listOf(AutofillEntryLookupTarget.DOMAIN),
      AutofillEntryLookupPolicy.lookupOrder(strategy, domain = "carpt.net")
    )
  }

  @Test fun browserWithoutDomainDoesNotUsePackageLookup() {
    val strategy =
      BrowserAutofillStrategyRegistry.forPackage("secure.unblock.unlimited.proxy.snap.hotspot.shield")

    assertEquals(
      emptyList<AutofillEntryLookupTarget>(),
      AutofillEntryLookupPolicy.lookupOrder(strategy, domain = null)
    )
  }

  @Test fun nonBrowserAlwaysUsesPackageLookup() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.example.app")

    assertEquals(
      listOf(AutofillEntryLookupTarget.PACKAGE),
      AutofillEntryLookupPolicy.lookupOrder(strategy, domain = "example.com")
    )
  }
}
