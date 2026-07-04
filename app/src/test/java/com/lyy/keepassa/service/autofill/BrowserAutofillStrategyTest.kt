/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserAutofillStrategyTest {

  @Test fun edgeUsesChromiumStrategyWithMaskedFieldFallback() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx")

    assertEquals(BrowserAutofillEngine.CHROMIUM, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertFalse(strategy.allowFocusedNonTextNodeFallback)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertTrue(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
    assertTrue(strategy.isSearchOrUrlFieldToken("url_bar"))
  }

  @Test fun kiwiProxyUsesDedicatedStrategyWithBrowserFormInference() {
    val strategy =
      BrowserAutofillStrategyRegistry.forPackage("secure.unblock.unlimited.proxy.snap.hotspot.shield")

    assertEquals(BrowserAutofillEngine.KIWI, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertFalse(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertTrue(strategy.allowFocusedNonTextNodeFallback)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertTrue(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
    assertTrue(strategy.isSearchOrUrlFieldToken("edtSearchURL"))
  }

  @Test fun firefoxUsesGeckoStrategyWithoutKiwiFormInference() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("org.mozilla.firefox")

    assertEquals(BrowserAutofillEngine.GECKO, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertFalse(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowFocusedNonTextNodeFallback)
    assertTrue(strategy.allowRequestFocusedIdFallback)
  }

  @Test fun androidBrowserUsesDedicatedWebViewStrategy() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.android.browser")

    assertEquals(BrowserAutofillEngine.ANDROID_BROWSER, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertFalse(strategy.allowFocusedNonTextNodeFallback)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertFalse(strategy.allowBrowserFormFieldInference)
    assertFalse(strategy.allowSingleFieldAuthFallback)
  }

  @Test fun adblockPlusBrowserUsesChromiumStrategy() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("org.adblockplus.browser")

    assertEquals(BrowserAutofillEngine.CHROMIUM, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
  }

  @Test fun yandexUsesDedicatedSingleFieldFallbackStrategy() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.yandex.browser")

    assertEquals(BrowserAutofillEngine.YANDEX, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertFalse(strategy.allowFocusedNonTextNodeFallback)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertFalse(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
  }

  @Test fun yandexDedicatedStrategyDoesNotRemoveLegacyChromiumMembership() {
    val chromiumPackagesField =
      BrowserAutofillStrategyRegistry::class.java.getDeclaredField("chromiumPackages")
    chromiumPackagesField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val chromiumPackages =
      chromiumPackagesField.get(BrowserAutofillStrategyRegistry) as Set<String>

    assertTrue(chromiumPackages.contains("com.yandex.browser"))
    assertEquals(
      BrowserAutofillEngine.YANDEX,
      BrowserAutofillStrategyRegistry.forPackage("com.yandex.browser").engine
    )
  }

  @Test fun idmBrowserUsesDedicatedStrategyWithFallbackPromptSupport() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("idm.internet.download.manager")

    assertEquals(BrowserAutofillEngine.IDM, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertTrue(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
  }

  @Test fun ucMobileIntlUsesDedicatedStrategyWithFallbackPromptSupport() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.UCMobile.intl")

    assertEquals(BrowserAutofillEngine.UC, strategy.engine)
    assertTrue(strategy.isBrowser)
    assertTrue(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertTrue(strategy.allowRequestFocusedIdFallback)
    assertTrue(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.allowSingleFieldAuthFallback)
  }

  @Test fun ucMobileIntlRecognizesLocalizedAddressBarHints() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.UCMobile.intl")

    assertTrue(strategy.isSearchOrUrlFieldToken("搜索或输入网址"))
    assertTrue(strategy.isSearchOrUrlFieldToken("输入网址"))
  }

  @Test fun unverifiedDeviceBrowsersUseConservativeBrowserStrategy() {
    listOf(
      "com.apusapps.browser",
      "com.explore.web.browser",
      "com.heytap.browser",
      "com.mi.globalbrowser",
      "com.mx.browser",
      "com.talpa.hibrowser",
      "com.uc.browser.en",
      "com.vivo.browser",
      "mobi.mgeek.TunnyBrowser",
      "net.fast.web.browser",
      "org.torproject.torbrowser"
    ).forEach { packageName ->
      val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)

      assertEquals("$packageName engine", expectedConservativeEngine(packageName), strategy.engine)
      assertTrue("$packageName browser", strategy.isBrowser)
      assertFalse("$packageName form inference", strategy.allowBrowserFormFieldInference)
      assertFalse("$packageName auth fallback", strategy.allowSingleFieldAuthFallback)
    }
  }

  @Test fun unknownPackageUsesConservativeDefaultStrategy() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.example.unknown")

    assertEquals(BrowserAutofillEngine.DEFAULT, strategy.engine)
    assertFalse(strategy.isBrowser)
    assertFalse(strategy.shouldClassifyNativeEditTextVirtualNodes)
    assertFalse(strategy.allowFocusedNonTextNodeFallback)
    assertFalse(strategy.allowRequestFocusedIdFallback)
    assertFalse(strategy.allowBrowserFormFieldInference)
    assertTrue(strategy.isSearchOrUrlFieldToken("search"))
  }

  private fun expectedConservativeEngine(packageName: String): BrowserAutofillEngine =
    when (packageName) {
      "org.torproject.torbrowser" -> BrowserAutofillEngine.GECKO
      else -> BrowserAutofillEngine.DEFAULT
    }
}
