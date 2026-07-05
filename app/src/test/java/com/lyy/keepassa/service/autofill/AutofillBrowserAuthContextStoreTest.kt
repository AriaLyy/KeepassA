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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

  @Test fun blankDomainDoesNotOverwriteExistingBrowserDomain() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "secure.unblock.unlimited.proxy.snap.hotspot.shield"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "carpt.net",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000
    )
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = " ",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 2_000
    )

    assertEquals(
      "carpt.net",
      AutofillBrowserAuthContextStore.find(packageName, 3_000)?.domain
    )
  }

  @Test fun emptyBrowserContextDoesNotOverwriteExistingFallbackAnchor() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "secure.unblock.unlimited.proxy.snap.hotspot.shield"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val fallbackId = mockk<AutofillId>()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "carpt.net",
      metadata = null,
      fallbackId = fallbackId,
      fallbackRole = BrowserFormFieldRole.PASSWORD,
      nowMs = 1_000
    )
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = " ",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 2_000
    )

    val context = AutofillBrowserAuthContextStore.find(packageName, 3_000)
    assertSame(fallbackId, context?.fallbackId)
    assertEquals(BrowserFormFieldRole.PASSWORD, context?.fallbackRole)
  }

  @Test fun currentFallbackWithoutDomainDoesNotCarryPreviousDomain() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.yandex.browser"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val previousFallbackId = mockk<AutofillId>()
    val currentFallbackId = mockk<AutofillId>()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "example.com",
      metadata = null,
      fallbackId = previousFallbackId,
      fallbackRole = BrowserFormFieldRole.PASSWORD,
      nowMs = 1_000
    )
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = " ",
      metadata = null,
      fallbackId = currentFallbackId,
      fallbackRole = null,
      nowMs = 2_000
    )

    val context = AutofillBrowserAuthContextStore.find(packageName, 3_000)
    assertSame(currentFallbackId, context?.fallbackId)
    assertNull(context?.fallbackRole)
    assertNull(context?.domain)
  }

  @Test fun ucCurrentFallbackWithoutDomainCarriesPreviousDomain() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.UCMobile.intl"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val previousFallbackId = mockk<AutofillId>()
    val currentFallbackId = mockk<AutofillId>()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "ubits.club",
      metadata = null,
      fallbackId = previousFallbackId,
      fallbackRole = BrowserFormFieldRole.PASSWORD,
      nowMs = 1_000
    )
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = " ",
      metadata = null,
      fallbackId = currentFallbackId,
      fallbackRole = null,
      nowMs = 2_000
    )

    val context = AutofillBrowserAuthContextStore.find(packageName, 3_000)
    assertSame(currentFallbackId, context?.fallbackId)
    assertNull(context?.fallbackRole)
    assertEquals("ubits.club", context?.domain)
  }

  @Test fun vivoCurrentFallbackWithoutDomainCarriesPreviousDomain() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.vivo.browser"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val previousFallbackId = mockk<AutofillId>()
    val currentFallbackId = mockk<AutofillId>()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "carpt.net",
      metadata = null,
      fallbackId = previousFallbackId,
      fallbackRole = BrowserFormFieldRole.PASSWORD,
      nowMs = 1_000
    )
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = " ",
      metadata = null,
      fallbackId = currentFallbackId,
      fallbackRole = null,
      nowMs = 2_000
    )

    val context = AutofillBrowserAuthContextStore.find(packageName, 3_000)
    assertSame(currentFallbackId, context?.fallbackId)
    assertNull(context?.fallbackRole)
    assertEquals("carpt.net", context?.domain)
  }

  @Test fun vivoCanRestoreDomainAfterAutofillServiceContextIsRecreated() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.vivo.browser"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val persistentStorage = FakePersistentDomainStorage()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "carpt.net",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000,
      persistentDomainStorage = persistentStorage
    )

    AutofillBrowserAuthContextStore.clear()

    assertEquals(
      "carpt.net",
      AutofillBrowserAuthContextStore.find(
        packageName = packageName,
        nowMs = 2_000,
        persistentDomainStorage = persistentStorage
      )?.domain
    )
  }

  @Test fun chromiumDoesNotPersistDomainForSingleFieldFallback() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.microsoft.emmx"
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val persistentStorage = FakePersistentDomainStorage()

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = "example.com",
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = 1_000,
      persistentDomainStorage = persistentStorage
    )

    AutofillBrowserAuthContextStore.clear()

    assertNull(
      AutofillBrowserAuthContextStore.find(
        packageName = packageName,
        nowMs = 2_000,
        persistentDomainStorage = persistentStorage
      )
    )
  }

  private class FakePersistentDomainStorage : AutofillBrowserPersistentDomainStorage {
    private val domains = HashMap<String, AutofillBrowserPersistentDomain>()

    override fun save(packageName: String, domain: String, nowMs: Long) {
      domains[packageName] = AutofillBrowserPersistentDomain(domain, nowMs)
    }

    override fun find(packageName: String): AutofillBrowserPersistentDomain? {
      return domains[packageName]
    }

    override fun clear(packageName: String) {
      domains.remove(packageName)
    }

    override fun clear() {
      domains.clear()
    }
  }
}
