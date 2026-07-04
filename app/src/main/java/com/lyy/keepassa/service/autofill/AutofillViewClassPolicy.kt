/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

internal object AutofillViewClassPolicy {

  fun isEditTextClassName(className: String?): Boolean {
    if (className.isNullOrBlank()) {
      return false
    }
    return className == "android.widget.EditText"
      || className.endsWith("EditText")
      || className.contains("TextInput", ignoreCase = true)
      || className.contains("AutoCompleteTextView", ignoreCase = true)
  }

  fun isWebViewClassName(className: String?): Boolean {
    if (className.isNullOrBlank()) {
      return false
    }
    return className == "android.webkit.WebView"
      || className.endsWith("WebView")
      || className.contains(".WebView")
  }
}
