/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillViewClassPolicyTest {

  @Test fun editTextClassNames_areRecognizedWithoutReflection() {
    assertTrue(AutofillViewClassPolicy.isEditTextClassName("android.widget.EditText"))
    assertTrue(AutofillViewClassPolicy.isEditTextClassName("androidx.appcompat.widget.AppCompatEditText"))
    assertTrue(AutofillViewClassPolicy.isEditTextClassName("com.google.android.material.textfield.TextInputEditText"))
  }

  @Test fun chromiumImageVirtualClass_isNotReflectedAsEditText() {
    assertFalse(AutofillViewClassPolicy.isEditTextClassName("android.widget.Image"))
  }

  @Test fun webViewClassNames_areRecognizedWithoutReflection() {
    assertTrue(AutofillViewClassPolicy.isWebViewClassName("android.webkit.WebView"))
    assertTrue(AutofillViewClassPolicy.isWebViewClassName("org.chromium.content.browser.webcontents.WebView"))
  }

  @Test fun nonInputClassNames_areIgnored() {
    assertFalse(AutofillViewClassPolicy.isEditTextClassName("android.widget.ImageView"))
    assertFalse(AutofillViewClassPolicy.isWebViewClassName("android.widget.Image"))
  }
}
