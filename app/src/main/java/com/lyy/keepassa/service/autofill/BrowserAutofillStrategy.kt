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
 *
 * [compatible]=false 表示该浏览器虽被策略注册表识别过,但实际 autofill 不可用
 * (典型原因:浏览器自身从未稳定触发 Autofill session)。列表展示时需要追加"不兼容"标记。
 */
data class SupportedBrowser(
  val packageName: String,
  val displayName: String,
  val engine: BrowserAutofillEngine,
  val compatible: Boolean = true
)

internal data class BrowserAutofillStrategy(
  val engine: BrowserAutofillEngine,
  val isBrowser: Boolean,
  val shouldClassifyNativeEditTextVirtualNodes: Boolean,
  val allowFocusedNonTextNodeFallback: Boolean,
  val allowRequestFocusedIdFallback: Boolean,
  val allowSearchOrUrlRequestFocusedIdFallback: Boolean = false,
  val preferRequestFocusedIdForAuthPromptFallback: Boolean = false,
  val disableSingleFieldFallbackDatasetFiltering: Boolean = false,
  val useDatasetAuthenticationForFallbackAuthPrompt: Boolean = false,
  val useDatasetAuthenticationForFallbackSearchPrompt: Boolean = false,
  val reuseStoredDomainForSingleFieldFallback: Boolean = false,
  val persistDomainForSingleFieldFallback: Boolean = false,
  val ignoreSearchOrUrlOnlyAutofillFields: Boolean = false,
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
    "com.hsv.freeadblockerbrowser",
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

  private val miBrowserPackages = setOf(
    "com.mi.globalbrowser"
  )

  private val heytapBrowserPackages = setOf(
    "com.heytap.browser"
  )

  private val vivoBrowserPackages = setOf(
    "com.vivo.browser"
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
    "com.apgsolutionsllc.APGSOLUTIONSLLC0007",
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
    "com.hsv.freeadblockerbrowser" to "Free Adblocker Browser",
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
    // Mi Browser
    "com.mi.globalbrowser" to "Mi Browser",
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
    "com.heytap.browser" to "HeyTap Browser",
    "com.vivo.browser" to "Vivo Browser",
    "com.apgsolutionsllc.APGSOLUTIONSLLC0007" to "Basic Web Browser",
    "com.mx.browser" to "Maxthon",
    "com.apusapps.browser" to "APUS Browser",
    "com.explore.web.browser" to "Explore Browser",
    "net.fast.web.browser" to "Fast Browser",
    "com.talpa.hibrowser" to "Talpa HiBrowser",
    "mobi.mgeek.TunnyBrowser" to "Tunny Browser"
  )

  /**
   * 已识别但实际 autofill 不可用的浏览器包名集合。
   *
   * Yandex:自始至终没稳定触发 Autofill session,onFillRequest 收不到,等于残废;
   * UC 国际版 (com.UCMobile.intl):UC 自研内核 + 屏蔽第三方 AutofillService 的虚拟节点结构,
   * 字段推断全部失配,实测无法填充。
   * HeyTap Browser (com.heytap.browser):已加入 Android Autofill 兼容包,也能创建系统
   * CompatibilityBridge,但在 45.14.4.1 真机网页登录页中,网页内容只暴露为空
   * FrameLayout;账号/密码输入框没有作为 EditText、密码节点或 WebView 虚拟 autofill
   * 节点出现在 AssistStructure/uiautomator 中。实测状态是键盘已显示且窗口焦点在 HeyTap,
   * 但 dumpsys autofill 仍为 No sessions,logcat 也没有 startSessionLocked() 或
   * AutoFillService.onFillRequest()。因此问题发生在系统/浏览器触发层,KeePassA 的字段推断、
   * 条目匹配和 FillResponse fallback 都没有机会执行。
   * Samsung Internet (com.sec.android.app.sbrowser):30.0.0.67 真机中窗口焦点和 IME 都已进入
   * Samsung 网页,系统 compat 也有 location_bar_edit_text,但 clean trigger 下没有稳定把网页登录框
   * 下发到 KeePassA;同时 Samsung 不暴露 Chromium ThirdPartyAutofill provider。保留 Chromium
   * 策略作为系统真正发送 FillRequest 时的 best-effort,设置页标记为不兼容。
   * Vivo Browser (com.vivo.browser):网页登录页能触发 Autofill session,但系统下发给 KeePassA 的
   * AssistStructure 只包含弱化后的虚拟字段 id,没有可用于域名匹配的公开来源。实测 dumpsys
   * autofill 中 mUrlBar=N/A,ViewNode.webDomain 为空,地址栏节点也没有被 Android Autofill compat
   * 识别为 URL bar;logcat 里虽然能看到 Vivo Browser 进程自己的
   * "url=https://carpt.net/login.php" 内部日志,但该值没有进入 AutofillService 可读取的
   * FillRequest/AssistStructure。官方 autofill-service compatibility-package 也只支持 name 和
   * maxLongVersionCode,不能为第三方服务配置 Vivo 的地址栏 resource id。由于浏览器场景严禁在
   * domain 缺失时回退到包名匹配,否则会把同一浏览器里的不同网站匹配到错误条目,因此 Vivo
   * 当前只能标记为不兼容,不再继续扩大 KeePassA 侧策略兜底。
   *
   * 策略代码(forPackage)保留对它们的识别,以便系统层一旦真的下发 FillRequest 时仍能尝试兜底;
   * 但在设置页"已适配浏览器"列表里必须明确标注"不兼容",避免用户误以为可用。
   */
  private val incompatiblePackages: Set<String> = setOf(
    "com.yandex.browser",
    "com.UCMobile.intl",
    "com.heytap.browser",
    "com.sec.android.app.sbrowser",
    "com.vivo.browser"
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
      val miBrowser = miBrowserPackages.map { it to BrowserAutofillEngine.CHROMIUM }
      val heytapBrowser = heytapBrowserPackages.map { it to BrowserAutofillEngine.ANDROID_BROWSER }
      val vivoBrowser = vivoBrowserPackages.map { it to BrowserAutofillEngine.DEFAULT }
      val uc = ucPackages.map { it to BrowserAutofillEngine.UC }
      val gecko = geckoPackages.map { it to BrowserAutofillEngine.GECKO }
      val android = androidBrowserPackages.map { it to BrowserAutofillEngine.ANDROID_BROWSER }
      val conservative = conservativeBrowserPackages.map { it to BrowserAutofillEngine.DEFAULT }
      return (chromium + kiwi + idm + miBrowser + heytapBrowser + vivoBrowser + uc + gecko + android + conservative)
        .map { (pkg, engine) ->
          SupportedBrowser(
            packageName = pkg,
            displayName = browserDisplayNames[pkg] ?: pkg,
            engine = engine,
            compatible = pkg !in incompatiblePackages
          )
        }
    }

  private val genericSearchOrUrlTokens = setOf(
    "search",
    "url",
    "url_bar",
    "location_bar",
    "address_bar",
    "address",
    // 网页搜索框常见 id/name:百度 = kw / wd,通用 = keyword / query / searchkey / searchword
    "kw",
    "keyword",
    "query",
    "searchkey",
    "searchword",
    // 中文搜索框 placeholder 常见关键字
    "搜索",
    "关键字",
    "关键词"
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

  private val miBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.CHROMIUM,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    // MI Global Browser creates an Autofill session, but can expose the focused web field as a
    // non-text WebView node. Keep this targeted so other Chromium browsers are not widened.
    allowFocusedNonTextNodeFallback = true,
    allowRequestFocusedIdFallback = true,
    // 用户聚焦 URL/搜索栏时不应触发自动填充;早期为了让 Mi Browser 偶发把 URL 当作唯一
    // 可聚焦节点时也能弹出 UI 而开启的兜底,反而让"地址栏聚焦"被误触发。回归正常行为,
    // 地址栏聚焦不再产生 auth prompt 锚点。
    allowSearchOrUrlRequestFocusedIdFallback = false,
    preferRequestFocusedIdForAuthPromptFallback = true,
    disableSingleFieldFallbackDatasetFiltering = true,
    useDatasetAuthenticationForFallbackAuthPrompt = true,
    ignoreSearchOrUrlOnlyAutofillFields = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val heytapBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.ANDROID_BROWSER,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    // HeyTap Browser uses com.android.browser activities and can expose masked metadata when the
    // Android framework actually delivers a FillRequest. Keep the strategy package-scoped so it
    // does not widen conservative browsers.
    //
    // Important: this strategy cannot force HeyTap to start an Autofill session. On tested HeyTap
    // 45.14.4.1 web login pages, the browser does not expose page username/password fields to the
    // framework, so Android never calls AutoFillService.onFillRequest(). The supported-browser list
    // marks HeyTap incompatible for that reason, while this strategy remains as a best-effort path
    // for builds/pages where the system does deliver a FillRequest.
    allowFocusedNonTextNodeFallback = true,
    allowRequestFocusedIdFallback = true,
    allowSearchOrUrlRequestFocusedIdFallback = false,
    preferRequestFocusedIdForAuthPromptFallback = true,
    disableSingleFieldFallbackDatasetFiltering = true,
    useDatasetAuthenticationForFallbackAuthPrompt = true,
    useDatasetAuthenticationForFallbackSearchPrompt = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens
  )

  private val vivoBrowserStrategy = BrowserAutofillStrategy(
    engine = BrowserAutofillEngine.DEFAULT,
    isBrowser = true,
    shouldClassifyNativeEditTextVirtualNodes = true,
    // Vivo Browser 10.8.3.4 能创建 Autofill session,但网页登录框下发给系统的
    // AssistStructure 元数据很弱:常见表现是 flags=128,虚拟 autofill id 有效,但字段
    // 没有稳定的 password/user hints,导致 conservative 策略下 autoFillIds 为空。
    // 只对 Vivo 启用浏览器表单位置推断和 request focused id 兜底,避免扩大其它保守浏览器。
    //
    // 该浏览器还会频繁触发 AutofillService unbind/destroy。部分页面先前请求能拿到域名,
    // 后续单字段请求只剩虚拟字段 id。这里允许按包名隔离、短 TTL 的域名持久缓存,
    // 但仍只按域名查库,不会在域名缺失时退回浏览器包名匹配。
    allowFocusedNonTextNodeFallback = false,
    allowRequestFocusedIdFallback = true,
    allowBrowserFormFieldInference = true,
    allowSingleFieldAuthFallback = true,
    reuseStoredDomainForSingleFieldFallback = true,
    persistDomainForSingleFieldFallback = true,
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
    reuseStoredDomainForSingleFieldFallback = true,
    searchOrUrlTokens = genericSearchOrUrlTokens + setOf(
      "搜索或输入网址",
      "输入网址",
      "网址",
      "搜索",
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
      in miBrowserPackages -> miBrowserStrategy
      in heytapBrowserPackages -> heytapBrowserStrategy
      in vivoBrowserPackages -> vivoBrowserStrategy
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
