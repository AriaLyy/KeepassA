/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeAutofillPromptPolicyTest {

  @Test fun enabledKeepassAutofillAndDisabledChromeThirdPartyMode_promptsUser() {
    assertTrue(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        chromeState = ChromeThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun disabledKeepassAutofill_doesNotPrompt() {
    assertFalse(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = false,
        chromeState = ChromeThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun enabledChromeThirdPartyMode_doesNotPrompt() {
    assertFalse(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        chromeState = ChromeThirdPartyAutofillState.ENABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun unknownChromeState_doesNotPrompt() {
    assertFalse(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        chromeState = ChromeThirdPartyAutofillState.UNKNOWN,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun cooldownSuppressesPrompt() {
    assertFalse(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        chromeState = ChromeThirdPartyAutofillState.DISABLED,
        isInCooldown = true,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun preOreo_doesNotPrompt() {
    assertFalse(
      ChromeAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        chromeState = ChromeThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.N_MR1
      )
    )
  }
}
