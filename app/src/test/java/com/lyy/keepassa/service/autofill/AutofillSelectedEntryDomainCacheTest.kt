/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import com.keepassdroid.database.PwEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSelectedEntryDomainCacheTest {

  @Test fun vivoSelectedEntryUrlStoresDomainAndKeepsFallbackAnchor() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.vivo.browser"
    val fallbackId = mockk<android.view.autofill.AutofillId>()
    val storage = FakePersistentDomainStorage()
    val entry = mockk<PwEntry> {
      every { url } returns "https://carpt.net/login.php"
    }

    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = BrowserAutofillStrategyRegistry.forPackage(packageName),
      domain = null,
      metadata = null,
      fallbackId = fallbackId,
      fallbackRole = BrowserFormFieldRole.USERNAME,
      nowMs = 1_000,
      persistentDomainStorage = storage
    )

    assertTrue(
      AutofillSelectedEntryDomainCache.remember(
        packageName = packageName,
        entry = entry,
        nowMs = 2_000,
        persistentDomainStorage = storage
      )
    )

    val context = AutofillBrowserAuthContextStore.find(
      packageName = packageName,
      nowMs = 2_100,
      persistentDomainStorage = storage
    )
    assertEquals("carpt.net", context?.domain)
    assertSame(fallbackId, context?.fallbackId)
    assertEquals("carpt.net", storage.find(packageName)?.domain)
  }

  @Test fun chromiumSelectedEntryUrlDoesNotUseVivoPersistentDomainCache() {
    AutofillBrowserAuthContextStore.clear()
    val packageName = "com.microsoft.emmx"
    val storage = FakePersistentDomainStorage()
    val entry = mockk<PwEntry> {
      every { url } returns "https://carpt.net/login.php"
    }

    assertFalse(
      AutofillSelectedEntryDomainCache.remember(
        packageName = packageName,
        entry = entry,
        nowMs = 2_000,
        persistentDomainStorage = storage
      )
    )
    assertEquals(null, storage.find(packageName))
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
