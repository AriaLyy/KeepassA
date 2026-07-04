/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.annotation.TargetApi
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import java.util.Locale

@TargetApi(Build.VERSION_CODES.O)
internal object UcBrowserAutofillCompatibility {

  private val ucAddressBarResourceIds = setOf(
    "address_input_search",
    "address_bar_go_search",
    "inputurl",
    "search_input",
    "search_bar",
    "search_edit_area",
    "search_copy_url",
    "search_input_scroll",
    "search_input_scroll_container"
  )

  fun isAddressBarNode(
    strategy: BrowserAutofillStrategy,
    viewNode: ViewNode
  ): Boolean {
    if (strategy.engine != BrowserAutofillEngine.UC) {
      return false
    }
    val idEntry = viewNode.idEntry?.lowercase(Locale.ROOT) ?: return false
    return idEntry in ucAddressBarResourceIds
  }

  fun canReuseStoredDomainForCurrentFallback(strategy: BrowserAutofillStrategy): Boolean {
    return strategy.engine == BrowserAutofillEngine.UC
  }
}
