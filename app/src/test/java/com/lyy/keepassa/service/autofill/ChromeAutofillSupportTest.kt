/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Test

class ChromeAutofillSupportTest {

  @Test fun providerUri_usesChromeThirdPartyModeContract() {
    assertEquals(
      "content://com.android.chrome.AutofillThirdPartyModeContentProvider/autofill_third_party_mode",
      ChromeAutofillSupport.providerUriString("com.android.chrome")
    )
  }

  @Test fun providerValueZero_meansThirdPartyModeDisabled() {
    assertEquals(
      ChromeThirdPartyAutofillState.DISABLED,
      ChromeAutofillSupport.stateFromProviderValue(0)
    )
  }

  @Test fun providerValueOne_meansThirdPartyModeEnabled() {
    assertEquals(
      ChromeThirdPartyAutofillState.ENABLED,
      ChromeAutofillSupport.stateFromProviderValue(1)
    )
  }

  @Test fun unknownProviderValue_isNotPromptable() {
    assertEquals(
      ChromeThirdPartyAutofillState.UNKNOWN,
      ChromeAutofillSupport.stateFromProviderValue(2)
    )
  }
}
