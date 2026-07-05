/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId

internal object AutofillSearchOrUrlFieldPolicy {

  fun shouldIgnoreClassifiedFields(
    strategy: BrowserAutofillStrategy,
    classifiedIds: Set<AutofillId>,
    searchOrUrlIds: Set<AutofillId>
  ): Boolean {
    return strategy.ignoreSearchOrUrlOnlyAutofillFields &&
      classifiedIds.isNotEmpty() &&
      classifiedIds.all(searchOrUrlIds::contains)
  }
}
