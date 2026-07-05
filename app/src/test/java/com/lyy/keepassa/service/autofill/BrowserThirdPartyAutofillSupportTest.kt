/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserThirdPartyAutofillSupportTest {

  @Test fun verifiedChromiumBrowsersExposeThirdPartyAutofillIntegration() {
    assertEquals(
      listOf(
        "com.android.chrome",
        "org.adblockplus.browser",
        "com.vivaldi.browser"
      ),
      BrowserThirdPartyAutofillSupport.integrations.map { it.packageName }
    )
  }

  @Test fun providerUri_usesPackageSpecificThirdPartyModeContract() {
    assertEquals(
      "content://com.android.chrome.AutofillThirdPartyModeContentProvider/autofill_third_party_mode",
      BrowserThirdPartyAutofillSupport.providerUriString("com.android.chrome")
    )
    assertEquals(
      "content://org.adblockplus.browser.AutofillThirdPartyModeContentProvider/autofill_third_party_mode",
      BrowserThirdPartyAutofillSupport.providerUriString("org.adblockplus.browser")
    )
    assertEquals(
      "content://com.vivaldi.browser.AutofillThirdPartyModeContentProvider/autofill_third_party_mode",
      BrowserThirdPartyAutofillSupport.providerUriString("com.vivaldi.browser")
    )
  }

  @Test fun providerValuesMapToThirdPartyModeStates() {
    assertEquals(
      BrowserThirdPartyAutofillState.DISABLED,
      BrowserThirdPartyAutofillSupport.stateFromProviderValue(0)
    )
    assertEquals(
      BrowserThirdPartyAutofillState.ENABLED,
      BrowserThirdPartyAutofillSupport.stateFromProviderValue(1)
    )
    assertEquals(
      BrowserThirdPartyAutofillState.UNKNOWN,
      BrowserThirdPartyAutofillSupport.stateFromProviderValue(2)
    )
  }

  @Test fun manifestDeclaresVerifiedBrowsersVisible() {
    val manifest = File("src/main/AndroidManifest.xml").readText()

    BrowserThirdPartyAutofillSupport.integrations.forEach { integration ->
      assertTrue(
        "${integration.packageName} package should be queryable",
        manifest.contains("<package android:name=\"${integration.packageName}\"")
      )
      assertTrue(
        "${integration.packageName} provider should be queryable",
        manifest.contains(
          "<provider android:authorities=\"${integration.providerAuthority}\""
        )
      )
    }
  }
}
