/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import com.keepassdroid.database.PwEntry

internal object AutofillSelectedEntryDomainCache {

  fun remember(
    packageName: String,
    entry: PwEntry,
    nowMs: Long = System.currentTimeMillis(),
    persistentDomainStorage: AutofillBrowserPersistentDomainStorage =
      CommonKvAutofillBrowserPersistentDomainStorage
  ): Boolean {
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    if (!strategy.persistDomainForSingleFieldFallback) {
      return false
    }
    val domain = AutofillBrowserUrlPolicy.extractDomainFromAddressValue(entry.url)
    if (domain == null) {
      return false
    }
    AutofillBrowserAuthContextStore.remember(
      packageName = packageName,
      strategy = strategy,
      domain = domain,
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      nowMs = nowMs,
      persistentDomainStorage = persistentDomainStorage
    )
    return true
  }
}
