/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

internal enum class AutofillEntryLookupTarget {
  DOMAIN,
  PACKAGE
}

internal object AutofillEntryLookupPolicy {

  fun lookupOrder(
    strategy: BrowserAutofillStrategy,
    domain: String?
  ): List<AutofillEntryLookupTarget> {
    if (strategy.isBrowser) {
      return if (!domain.isNullOrBlank()) {
        listOf(AutofillEntryLookupTarget.DOMAIN)
      } else {
        emptyList()
      }
    }
    return listOf(AutofillEntryLookupTarget.PACKAGE)
  }
}
