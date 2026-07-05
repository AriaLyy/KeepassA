/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import android.content.Context
import android.content.Intent

typealias ChromeThirdPartyAutofillState = BrowserThirdPartyAutofillState

object ChromeAutofillPromptPolicy {
  fun shouldPrompt(
    isKeepassAutofillEnabled: Boolean,
    chromeState: ChromeThirdPartyAutofillState,
    isInCooldown: Boolean,
    sdkInt: Int
  ): Boolean {
    return BrowserThirdPartyAutofillPromptPolicy.shouldPrompt(
      isKeepassAutofillEnabled = isKeepassAutofillEnabled,
      browserState = chromeState,
      isInCooldown = isInCooldown,
      sdkInt = sdkInt
    )
  }
}

object ChromeAutofillSupport {
  const val CHROME_PACKAGE = BrowserThirdPartyAutofillSupport.CHROME_PACKAGE

  fun providerUriString(packageName: String = CHROME_PACKAGE): String {
    return BrowserThirdPartyAutofillSupport.providerUriString(packageName)
  }

  fun stateFromProviderValue(value: Int): ChromeThirdPartyAutofillState {
    return BrowserThirdPartyAutofillSupport.stateFromProviderValue(value)
  }

  fun thirdPartyModeState(
    context: Context,
    packageName: String = CHROME_PACKAGE
  ): ChromeThirdPartyAutofillState {
    return BrowserThirdPartyAutofillSupport.thirdPartyModeState(context, packageName)
  }

  fun settingsIntent(packageName: String = CHROME_PACKAGE): Intent {
    return BrowserThirdPartyAutofillSupport.settingsIntent(packageName)
  }

  fun openSettings(
    context: Context,
    packageName: String = CHROME_PACKAGE
  ): Boolean {
    return BrowserThirdPartyAutofillSupport.openSettings(context, packageName)
  }
}
