/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.util.Locale

enum class BrowserAutofillEngine {
  CHROMIUM,
  YANDEX,
  KIWI,
  IDM,
  UC,
  GECKO,
  ANDROID_BROWSER,
  DEFAULT
}

/**
 * 已适配的浏览器描述。供设置页"已适配浏览器"列表展示用。
 */
data class SupportedBrowser(
  val packageName: String,
  val displayName: String,
  val engine: BrowserAutofillEngine
)

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
    // "com.yandex.browser",
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

  private val yandexPackages = setOf(
    "com.yandex.browser"
  )

  private val kiwiPackages = setOf(
    "com.kiwibrowser.browser",
    "secure.unblock.unlimited.proxy.snap.hotspot.shield"
  )

  private val idmPackages = setOf(
    "idm.internet.download.manager"
  )

  private val ucPackages = setOf(
    "com.UCMobile.intl"
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
    "com.uc.browser.en",
    "com.mi.globalbrowser",
    "com.heytap.browser",
    "com.vivo.browser",
    "com.mx.browser",
    "com.apusapps.browser",
    "com.explore.web.browser",
    "net.fast.web.browser",
    "com.talpa.hibrowser",
    "mobi.mgeek.TunnyBrowser"
  )

  /**
   * 包名 → 展示名映射。未命中的包名直接显示包名本身。
   * 仅用于设置页"已适配浏览器"列表的展示,不影响策略匹配。
   */
  private val browserDisplayNames: Map<String, String> = mapOf(
    // Chromium 系
    "com.microsoft.emmx" to "Microsoft Edge",
    "com.android.chrome" to "Google Chrome",
    "com.chrome.beta" to "Chrome Beta",
    "com.chrome.dev" to "Chrome Dev",
    "com.chrome.canary" to "Chrome Canary",
    "com.google.android.apps.chrome" to "Chrome",
    "com.google.android.apps.chrome_dev" to "Chrome Dev",
    "com.brave.browser" to "Brave Browser",
    "org.adblockplus.browser" to "Adblock Browser",
    "com.opera.browser" to "Opera Browser",
    "com.opera.browser.beta" to "Opera Beta",
    "com.opera.mini.native" to "Opera Mini",
    "com.opera.mini.native.beta" to "Opera Mini Beta",
    "com.opera.touch" to "Opera Touch",
    "com.sec.android.app.sbrowser" to "Samsung Internet",
    "com.sec.android.app.sbrowser.beta" to "Samsung Internet Beta",
    "com.amazon.cloud9" to "Amazon Silk",
    "mark.via.gp" to "Via Browser (GP)",
    "mark.via" to "Via Browser",
    "org.bromite.bromite" to "Bromite",
    "org.chromium.chrome" to "Chromium",
    "com.ecosia.android" to "Ecosia",
    "com.qwant.liberty" to "Qwant Liberty",
    "com.vivaldi.browser" to "Vivaldi",
    "com.mmbox.xbrowser" to "X Browser",
    "info.torapp.uweb" to "Uweb Browser",
    // Yandex
    "com.yandex.browser" to "Yandex Browser",
    // Kiwi
    "com.kiwibrowser.browser" to "Kiwi Browser",
    "secure.unblock.unlimited.proxy.snap.hotspot.shield" to "Snap VPN Browser",
    // IDM
    "idm.internet.download.manager" to "IDM+",
    // UC
    "com.UCMobile.intl" to "UC Browser",
    // Gecko
    "org.mozilla.firefox" to "Firefox",
    "org.mozilla.firefox_beta" to "Firefox Beta",
    "org.mozilla.fennec_aurora" to "Fennec Aurora",
    "org.mozilla.fennec_fdroid" to "Fennec F-Droid",
    "org.mozilla.fenix" to "Fenix",
    "org.mozilla.fenix.nightly" to "Fenix Nightly",
    "org.mozilla.reference.browser" to "Firefox Reference",
    "org.mozilla.rocket" to "Firefox Rocket",
    "org.torproject.torbrowser" to "Tor Browser",
    // AOSP
    "com.android.browser" to "AOSP Browser",
    "org.codeaurora.swe.browser" to "SWE Browser",
    // Conservative
    "com.uc.browser.en" to "UC Browser HD",
    "com.mi.globalbrowser" to "Mi Browser",
    "com.heytap.browser" to "HeyTap Browser",
    "com.vivo.browser" to "Vivo Browser",
    "com.mx.browser" to "Maxthon",
    "com.apusapps.browser" to "APUS Browser",
    "com.explore.web.browser" to "Explore Browser",
    "net.fast.web.browser" to "Fast Browser",
    "com.talpa.hibrowser" to "Talpa HiBrowser",
    "mobi.mgeek.TunnyBrowser" to "Tunny Browser"
  )

  /**
   * 所有已适配的浏览器列表,按 engine 分组、组内按展示名排序。供设置页展示。
   */
  val supportedBrowsers: List<SupportedBrowser>
    get() {
      val chromium = chromiumPackages.map {
        it to effectiveEngineForPackage(it, BrowserAutofillEngine.CHROMIUM)
      }
      val kiwi = kiwiPackages.map { it to BrowserAutofillEngine.KIWI }
      val idm = idmPackages.map { it to BrowserAutofillEngine.IDM }
      val uc = ucPackages.map { it to BrowserAutofillEngine.UC }
      val gecko = geckoPackages.map { it to BrowserAutofillEngine.GECKO }
      val android = androidBrowserPackages.map { it to BrowserAutofillEngine.ANDROID_BROWSER }
      val conservative = conservativeBrowserPackages.map { it to BrowserAutofillEngine.DEFAULT }
      return (chromium + kiwi + idm + uc + gecko + android + conservative)
        .map { (pkg, engine) ->
          SupportedBrowser(
            packageName = pkg,
            displayName = browserDisplayNames[pkg] ?: pkg,
            engine = engine
          )
        }
    }

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

  private val yandexStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.YANDEX,
    isBrowser = true,
    // Yandex Browser 需要独立记录,但当前真机证据显示它的问题多数发生在
    // AutofillService.onFillRequest() 之前:浏览器没有稳定创建 Android Autofill session。
    // 因此该策略只用于"系统已经把 FillRequest 交给 KeePassA"的场景,不能强制 Yandex 弹出
    // 自动填充 UI。不要把未触发 session 的问题继续归因到策略匹配或字段推断。
    //
    // 策略保持保守:允许当前 focused id 做单字段认证兜底,但不启用 Chromium 的表单字段推断,
    // 避免在 Yandex 的网页/iframe 暴露不完整时扩大误填风险。
    shouldClassifyNativeEditTextVirtualNodes = true,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = false,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val packageStrategyOverrides: Map<String, BrowserAutofillStrategy> =
    yandexPackages.associateWith { yandexStrategy }

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

  private val idmStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.IDM,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val ucStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.UC,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens + setOf(
      "搜索或输入网址",
      "输入网址",
      "网址",
      "搜索",
      "豆瓣"
    )
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
    packageStrategyOverrides[pkgName]?.let {
      return it
    }
    return when (pkgName) {
      in chromiumPackages -> chromiumStrategy
      in kiwiPackages -> kiwiStrategy
      in idmPackages -> idmStrategy
      in ucPackages -> ucStrategy
      in geckoPackages -> geckoStrategy
      in androidBrowserPackages -> androidBrowserStrategy
      in conservativeBrowserPackages -> conservativeBrowserStrategy
      else -> if (W3cHints.isBrowser(pkgName)) conservativeBrowserStrategy else nonBrowserStrategy
    }
  }

  private fun effectiveEngineForPackage(
    packageName: String,
    defaultEngine: BrowserAutofillEngine
  ): BrowserAutofillEngine = packageStrategyOverrides[packageName]?.engine ?: defaultEngine
}
