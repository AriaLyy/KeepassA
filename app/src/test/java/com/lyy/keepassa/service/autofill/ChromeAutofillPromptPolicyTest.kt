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

  @Test fun enabledKeepassAutofillAndDisabledBrowserThirdPartyMode_promptsUser() {
    assertTrue(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        browserState = BrowserThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun disabledKeepassAutofill_doesNotPrompt() {
    assertFalse(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = false,
        browserState = BrowserThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun enabledBrowserThirdPartyMode_doesNotPrompt() {
    assertFalse(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        browserState = BrowserThirdPartyAutofillState.ENABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun unknownBrowserState_doesNotPrompt() {
    assertFalse(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        browserState = BrowserThirdPartyAutofillState.UNKNOWN,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun cooldownSuppressesPrompt() {
    assertFalse(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        browserState = BrowserThirdPartyAutofillState.DISABLED,
        isInCooldown = true,
        sdkInt = Build.VERSION_CODES.O
      )
    )
  }

  @Test fun preOreo_doesNotPrompt() {
    assertFalse(
      BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
        isKeepassAutofillEnabled = true,
        browserState = BrowserThirdPartyAutofillState.DISABLED,
        isInCooldown = false,
        sdkInt = Build.VERSION_CODES.N_MR1
      )
    )
  }
}
