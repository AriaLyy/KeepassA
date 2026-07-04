/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import KDBAutoFillRepository
import com.keepassdroid.database.PwEntry

internal object AutofillEntryLookup {

  fun find(
    packageName: String,
    domain: String?
  ): MutableList<PwEntry>? {
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    val normalizedDomain = domain?.trim()?.takeIf { it.isNotEmpty() }
    for (target in AutofillEntryLookupPolicy.lookupOrder(strategy, normalizedDomain)) {
      val entries = when (target) {
        AutofillEntryLookupTarget.DOMAIN ->
          KDBAutoFillRepository.getAutoFillDataByDomain(normalizedDomain!!)
        AutofillEntryLookupTarget.PACKAGE ->
          KDBAutoFillRepository.getAutoFillDataByPackageName(packageName)
      }
      if (!entries.isNullOrEmpty()) {
        return entries
      }
    }
    return null
  }
}
