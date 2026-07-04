/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillBrowserAuthContextStoreTest {

  @Test fun browserPackageStoresDomainForPostAuthLookup() {
    AutofillBrowserAuthContextStore.clear()

    AutofillBrowserAuthContextStore.remember(
      packageName = "secure.unblock.unlimited.proxy.snap.hotspot.shield",
      strategy = BrowserAutofillStrategyRegistry.forPackage(
        "secure.unblock.unlimited.proxy.snap.hotspot.shield"
      ),
      domain = "carpt.net",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000
    )

    assertEquals(
      "carpt.net",
      AutofillBrowserAuthContextStore.find("secure.unblock.unlimited.proxy.snap.hotspot.shield", 2_000)?.domain
    )
  }

  @Test fun nonBrowserPackageDoesNotStoreAuthContext() {
    AutofillBrowserAuthContextStore.clear()

    AutofillBrowserAuthContextStore.remember(
      packageName = "com.example.app",
      strategy = BrowserAutofillStrategyRegistry.forPackage("com.example.app"),
      domain = "example.com",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000
    )

    assertNull(AutofillBrowserAuthContextStore.find("com.example.app", 2_000))
  }

  @Test fun expiredContextIsIgnored() {
    AutofillBrowserAuthContextStore.clear()

    AutofillBrowserAuthContextStore.remember(
      packageName = "com.microsoft.emmx",
      strategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx"),
      domain = "example.com",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000
    )

    assertNull(
      AutofillBrowserAuthContextStore.find(
        packageName = "com.microsoft.emmx",
        nowMs = 1_000 + AutofillBrowserAuthContextStore.TTL_MS + 1
      )
    )
  }
}
