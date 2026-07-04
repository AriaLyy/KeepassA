/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.util.Locale

internal enum class BrowserAutofillEngine {
  CHROMIUM,
  KIWI,
  GECKO,
  ANDROID_BROWSER,
  DEFAULT
}

internal data class BrowserAutofillStrategy(
  val engine: BrowserAutofillEngine,
  val isBrowser: Boolean,
  val shouldClassifyNativeEditTextVirtualNodes: Boolean,
  val allowFocusedNonTextNodeFallback: Boolean,
  val allowRequestFocusedIdFallback: Boolean,
  val allowBrowserFormFieldInference: Boolean,
  val allowSingleFieldAuthFallback: Boolean,
  private val searchOrUrlTokens: Set<String>
) {

  fun isSearchOrUrlFieldToken(token: String): Boolean {
    val normalized = token.lowercase(Locale.ROOT)
    return searchOrUrlTokens.any { normalized == it || normalized.contains(it) }
  }
}

internal object BrowserAutofillStrategyRegistry {

  private val chromiumPackages = setOf(
    "com.microsoft.emmx",
    "com.android.chrome",
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary",
    "com.google.android.apps.chrome",
    "com.google.android.apps.chrome_dev",
    "com.brave.browser",
    "org.adblockplus.browser",
    "com.opera.browser",
    "com.opera.browser.beta",
    "com.opera.mini.native",
    "com.opera.mini.native.beta",
    "com.opera.touch",
    "com.yandex.browser",
    "com.sec.android.app.sbrowser",
    "com.sec.android.app.sbrowser.beta",
    "com.amazon.cloud9",
    "mark.via.gp",
    "mark.via",
    "org.bromite.bromite",
    "org.chromium.chrome",
    "com.ecosia.android",
    "com.qwant.liberty",
    "com.vivaldi.browser",
    "com.mmbox.xbrowser",
    "info.torapp.uweb"
  )

  private val kiwiPackages = setOf(
    "com.kiwibrowser.browser",
    "secure.unblock.unlimited.proxy.snap.hotspot.shield"
  )

  private val geckoPackages = setOf(
    "org.mozilla.firefox",
    "org.mozilla.firefox_beta",
    "org.mozilla.fennec_aurora",
    "org.mozilla.fennec_fdroid",
    "org.mozilla.fenix",
    "org.mozilla.fenix.nightly",
    "org.mozilla.reference.browser",
    "org.mozilla.rocket",
    "org.torproject.torbrowser"
  )

  private val androidBrowserPackages = setOf(
    "com.android.browser",
    "org.codeaurora.swe.browser"
  )

  private val conservativeBrowserPackages = setOf(
    "com.UCMobile.intl",
    "com.uc.browser.en",
    "com.mi.globalbrowser",
    "com.heytap.browser",
    "com.vivo.browser",
    "com.mx.browser",
    "com.apusapps.browser",
    "com.explore.web.browser",
    "net.fast.web.browser",
    "idm.internet.download.manager",
    "com.talpa.hibrowser",
    "mobi.mgeek.TunnyBrowser"
  )

  private val genericSearchOrUrlTokens = setOf(
    "search",
    "url",
    "url_bar",
    "location_bar",
    "address_bar",
    "address"
  )

  private val chromiumStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.CHROMIUM,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    // Chrome 自 v100+ 起会对第三方 AutofillService 屏蔽密码框的 inputType/autofillHints,
    // 导致 isPassword/isUserName 全部失配 → autoFillFields 为空。
    // 启用与 Kiwi 一致的兜底:字段推断、单字段 auth、聚焦 id 兜底,
    // 让 Chrome mask 字段后仍能按位置/特征识别并弹出提示。
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val kiwiStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.KIWI,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = false,
    allowFocusedNonTextNodeFallback = true,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens + "edtsearchurl"
  )

  private val geckoStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.GECKO,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = false,
    allowFocusedNonTextNodeFallback = true,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = false,
    allowSingleFieldAuthFallback = false,
    searchOrUrlTokens = genericSearchOrUrlTokens + "edtsearchurl"
  )

  private val androidBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.ANDROID_BROWSER,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = false,
    allowSingleFieldAuthFallback = false,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val conservativeBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.DEFAULT,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = false,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = false,
    allowBrowserFormFieldInference = false,
    allowSingleFieldAuthFallback = false,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val nonBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.DEFAULT,
    isBrowser = false,
    shouldClassifyNativeEditTextVirtualNodes = false,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = false,
    allowBrowserFormFieldInference = false,
    allowSingleFieldAuthFallback = false,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  fun forPackage(pkgName: String?): BrowserAutofillStrategy {
    if (pkgName.isNullOrEmpty()) {
      return nonBrowserStrategy
    }
    return when (pkgName) {
      in chromiumPackages -> chromiumStrategy
      in kiwiPackages -> kiwiStrategy
      in geckoPackages -> geckoStrategy
      in androidBrowserPackages -> androidBrowserStrategy
      in conservativeBrowserPackages -> conservativeBrowserStrategy
      else -> if (W3cHints.isBrowser(pkgName)) conservativeBrowserStrategy else nonBrowserStrategy
    }
  }
}
