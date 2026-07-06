/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import androidx.autofill.HintConstants
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadata
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadataCollection
import timber.log.Timber
import java.util.Locale

/**
 * Parser for an AssistStructure object. This is invoked when the Autofill Service receives an
 * AssistStructure from the client Activity, representing its View hierarchy. In this sample, it
 * parses the hierarchy and collects autofill metadata from {@link ViewNode}s along the way.
 */
@TargetApi(Build.VERSION_CODES.O)
internal class StructureParser(private val autofillStructure: AssistStructure) {
  val autoFillFields = AutoFillFieldMetadataCollection()
  val useFields = ArrayList<ViewNode>()
  val passFields = ArrayList<ViewNode>()
  val totpFields = ArrayList<ViewNode>()
  val searchOrUrlAutoFillIds = HashSet<AutofillId>()
  private val browserFormFieldCandidates = ArrayList<ViewNode>()
  var domainUrl = ""
  var pkgName = ""
  var isW3c = false
  var isInnerAppW3c = false
  var authPromptFallbackId: AutofillId? = null
    private set
  var authPromptFallbackRole: BrowserFormFieldRole? = null
    private set
  private var authPromptFallbackIdFocused = false
  private var browserStrategy = BrowserAutofillStrategyRegistry.forPackage(null)

  companion object {
    // 其它应用editText 可能设置的id名，如：R.id.email
    val usernameHints = HashSet<String>().also {
      it.add("email")
      it.add("e-email")
      it.add("account")
      it.add("user_name")
      it.add("mobile")
      it.add("user id")
      it.add(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
      it.add(HintConstants.AUTOFILL_HINT_PHONE)
      it.add(HintConstants.AUTOFILL_HINT_NAME)
      it.add(HintConstants.AUTOFILL_HINT_USERNAME)
      it.add(HintConstants.AUTOFILL_HINT_PERSON_NAME)
      it.add(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)
      it.add(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
      it.add(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS)
      it.add(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
      // 中文凭证关键字 — 用于识别中文登录页面的用户名输入框,
      // 也作为 isFocusedUnmarkedBrowserInput 的护栏:hint 含这些词的字段
      // 走原有 isUserName 流程,不会落到 TOTP 兜底。
      it.add("账号")
      it.add("账户")
      it.add("户名")
      it.add("用户名")
      it.add("用户")
      it.add("邮箱")
      it.add("电子邮箱")
      it.add("手机")
      it.add("电话")
    }

    val passHints = HashSet<String>().also {
      it.add(HintConstants.AUTOFILL_HINT_PASSWORD)
      it.add(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
      it.add("passwort")
      it.add("passwordAuto")
      it.add("pswd")
      it.add("密码")
    }

    /**
     * key: class name, value: isEditText
     */
    val editTextMap = HashSet<String>()

    /**
     * key: class name, value: WebView
     */
    val webViewMap = HashSet<String>()
  }

  private fun clear() {
    autoFillFields.clear()
    useFields.clear()
    passFields.clear()
    totpFields.clear()
    searchOrUrlAutoFillIds.clear()
    browserFormFieldCandidates.clear()
    authPromptFallbackId = null
    authPromptFallbackRole = null
    authPromptFallbackIdFocused = false
  }

  /**
   * 是否是用户手动 用户手机选择了自动填充，也就是editText获取了焦点才开始弹出
   * @param pkgName 目标应用包名
   */
  fun parseForFill(
    isManual: Boolean,
    pkgName: String
  ) {
    this.pkgName = pkgName
    browserStrategy = BrowserAutofillStrategyRegistry.forPackage(pkgName)
    safeParse({ parse(isManual) }, { clear() })
  }

  /**
   * Traverse AssistStructure and add ViewNode metadata to a flat list.
   */
  private fun parse(isManual: Boolean) {
    isW3c = false
    domainUrl = ""
    Timber.d("Parsing structure for ${autofillStructure.activityComponent}")
    val nodeSize = autofillStructure.windowNodeCount
    clear()
    for (i in 0 until nodeSize) {
      parseLocked(autofillStructure.getWindowNodeAt(i).rootViewNode)
    }
    applyBrowserFallbackCredentialFields()
    applyTotpDisambiguation()
    // 如果密码为空，默认不弹出选择item，这是为了防止遇到editText就弹出item的情况
    if (passFields.isEmpty() && totpFields.isEmpty() && !isManual && !isW3c) {
      autoFillFields.clear()
    }
  }

  /**
   * 多 TOTP 候选时的歧义消解(纯扩展,单候选场景不影响)。
   *
   * 背景:某些页面会同时出现"验证码"(可能是图片验证码 / 短信验证码)和"两步验证"
   * (真正的 TOTP)输入框,而 chineseTokens 同时包含"验证码"和"两步验证",导致两个字段
   * 都被识别为 TOTP,填充时把 TOTP 错填到"验证码"框。
   *
   * 策略:若候选中存在"高置信度 TOTP token"(两步验证 / 二次验证 / 动态码 / 动态密码 /
   * 一次性密码 / otp / totp / 2fa / mfa / authenticator / onetimecode),则丢弃只匹配
   * 通用 token(验证码 / code)的字段。所有候选都是 specific 或都是 generic 时不处理。
   */
  private fun applyTotpDisambiguation() {
    if (totpFields.size <= 1) return

    val (specific, generic) = totpFields.partition(::hasSpecificTotpToken)
    if (specific.isEmpty() || generic.isEmpty()) return

    Timber.d("totp disambiguation: ${specific.size} specific + ${generic.size} generic, narrowing to specific")
    generic.forEach { f ->
      val autofillId = f.autofillId
      if (autofillId != null) {
        autoFillFields.removeField(autofillId, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP)
      }
      Timber.d("totp disambiguation: drop generic hint=${f.hint} idEntry=${f.idEntry} autofillId=${autofillId}")
    }
    totpFields.clear()
    totpFields.addAll(specific)
  }

  private fun hasSpecificTotpToken(f: ViewNode): Boolean {
    val tokens = ArrayList<String>()
    f.autofillHints?.forEach(tokens::add)
    f.idEntry?.let(tokens::add)
    f.hint?.toString()?.let(tokens::add)
    f.htmlInfo?.attributes?.forEach { attr ->
      attr.first?.let(tokens::add)
      attr.second?.let(tokens::add)
    }
    return tokens.any(::isSpecificTotpToken)
  }

  private fun isSpecificTotpToken(value: CharSequence?): Boolean {
    val raw = value?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val lower = raw.lowercase(Locale.ROOT)
    val specificChinese = listOf("两步验证", "二次验证", "动态码", "动态密码", "一次性密码")
    if (specificChinese.any { lower.contains(it) }) return true
    val normalized = lower.replace(Regex("[^a-z0-9]"), "")
    if (normalized.isEmpty()) return false
    val specificEng = listOf("otp", "totp", "2fa", "mfa", "authenticator", "onetimecode", "twostep", "twofactor")
    return specificEng.any { normalized == it || normalized.contains(it) }
  }

  private fun parseLocked(viewNode: ViewNode) {
    // 尽早捕获 domainUrl,避免 AutoFillService 因 domainUrl 为空回退到按包名匹配(浏览器场景下匹配错误)
    if (domainUrl.isBlank() && !viewNode.webDomain.isNullOrEmpty()) {
      AutofillBrowserUrlPolicy.normalizeDomain(viewNode.webDomain)?.let { domain ->
        domainUrl = domain
        W3cHints.curDomainUrl = domainUrl
        Timber.d("domainUrl = $domainUrl")
        ImeBrowserDomainContext.remember(
          browserPackage = pkgName,
          domain = domainUrl,
          source = ImeBrowserDomainContext.Source.WEB_DOMAIN
        )
      }
    }
    rememberBrowserAddressFieldDomain(viewNode)
    rememberAuthPromptFallbackId(viewNode)
    rememberBrowserFormFieldCandidate(viewNode)

    if (browserStrategy.isBrowser) {
      // 浏览器场景:HTML input 通常带 htmlInfo,走 W3C 路径
      checkW3C(viewNode)
      if (isW3c) {
        getW3CInfo(viewNode)
      }
      // Edge/Chrome 等基于自有 Chromium 的浏览器,会把 HTML input 暴露成原生 EditText
      // 虚拟视图(无 htmlInfo、tag=null),需要按原生 EditText 逻辑识别
      val className = viewNode.className
      if (browserStrategy.shouldClassifyNativeEditTextVirtualNodes &&
        classIsEditText(className) &&
        !isLikelySearchOrUrlField(viewNode)
      ) {
        getAndroidViewInfo(viewNode)
      }
    } else {
      // 原生 App 场景
      if (!viewNode.autofillHints.isNullOrEmpty()) {
        getAndroidViewInfo(viewNode)
      } else {
        val className = viewNode.className
        if (classIsEditText(className)) {
          getAndroidViewInfo(viewNode)
        } else if (classIsWebView(className)) {
          innerAppWebView(viewNode)
          return
        }
      }
    }

    val childrenSize = viewNode.childCount
    for (i in 0 until childrenSize) {
      parseLocked(viewNode.getChildAt(i))
    }
  }

  private fun rememberAuthPromptFallbackId(viewNode: ViewNode) {
    val autofillId = viewNode.autofillId ?: return
    if (isLikelySearchOrUrlField(viewNode)) {
      searchOrUrlAutoFillIds.add(autofillId)
      return
    }
    if (!isAuthPromptFallbackCandidate(viewNode)) {
      return
    }

    val isFocusedNode = viewNode.isFocused || viewNode.isAccessibilityFocused
    if (authPromptFallbackId != null && (authPromptFallbackIdFocused || !isFocusedNode)) {
      return
    }

    authPromptFallbackId = autofillId
    authPromptFallbackRole = if (isPassword(viewNode)) {
      BrowserFormFieldRole.PASSWORD
    } else {
      BrowserFormFieldRole.USERNAME
    }
    authPromptFallbackIdFocused = isFocusedNode
    Timber.d(
      "auth prompt fallback id = $autofillId, isFocused = $isFocusedNode, idEntry = ${viewNode.idEntry}, hint = ${viewNode.hint}"
    )
  }

  private fun rememberBrowserAddressFieldDomain(viewNode: ViewNode) {
    if (!browserStrategy.isBrowser || domainUrl.isNotBlank()) {
      return
    }

    val isUcAddressBarNode =
      UcBrowserAutofillCompatibility.isAddressBarNode(browserStrategy, viewNode)
    val extractedDomain = if (isUcAddressBarNode) {
      extractUcAddressBarDomain(viewNode)
    } else if (isLikelySearchOrUrlField(viewNode)) {
      extractAddressFieldDomain(viewNode)
    } else {
      null
    }
    if (isUcAddressBarNode) {
      Timber.d(
        "UC address bar candidate idEntry = ${viewNode.idEntry}, domainExtracted = ${extractedDomain != null}, hasText = ${!viewNode.text.isNullOrBlank()}, hasContentDescription = ${!viewNode.contentDescription.isNullOrBlank()}"
      )
    }
    val domain: String = extractedDomain ?: return

    domainUrl = domain
    W3cHints.curDomainUrl = domain
    Timber.d("domainUrl = $domainUrl")
    ImeBrowserDomainContext.remember(
      browserPackage = pkgName,
      domain = domainUrl,
      source = ImeBrowserDomainContext.Source.ADDRESS_BAR
    )
  }

  private fun extractAddressFieldDomain(viewNode: ViewNode): String? {
    return AutofillBrowserUrlPolicy.extractDomainFromAddressValue(
      viewNode.autofillValue?.takeIf { it.isText }?.textValue
    ) ?: AutofillBrowserUrlPolicy.extractDomainFromAddressValue(viewNode.text)
      ?: extractDomainFromHtmlValueAttribute(viewNode)
  }

  private fun extractUcAddressBarDomain(viewNode: ViewNode): String? {
    return extractAddressFieldDomain(viewNode)
      ?: AutofillBrowserUrlPolicy.extractDomainFromAddressValue(viewNode.contentDescription)
  }

  private fun extractDomainFromHtmlValueAttribute(viewNode: ViewNode): String? {
    return viewNode.htmlInfo?.attributes
      ?.firstOrNull { it.first.equals("value", ignoreCase = true) }
      ?.second
      ?.let(AutofillBrowserUrlPolicy::extractDomainFromAddressValue)
  }

  private fun isAuthPromptFallbackCandidate(viewNode: ViewNode): Boolean {
    return AutofillFallbackFieldPolicy.canAnchorAuthPrompt(
      autofillType = viewNode.autofillType,
      isAssistBlocked = viewNode.isAssistBlocked,
      isFocused = viewNode.isFocused,
      isAccessibilityFocused = viewNode.isAccessibilityFocused,
      isHtmlInput = viewNode.htmlInfo?.tag.equals("input", ignoreCase = true),
      className = viewNode.className?.toString(),
      allowFocusedNonTextNodeFallback = browserStrategy.allowFocusedNonTextNodeFallback
    )
  }

  private fun rememberBrowserFormFieldCandidate(viewNode: ViewNode) {
    if (!browserStrategy.allowBrowserFormFieldInference) {
      return
    }
    if (viewNode.autofillId == null || viewNode.isAssistBlocked) {
      return
    }
    if (viewNode.autofillType != View.AUTOFILL_TYPE_TEXT || viewNode.visibility != View.VISIBLE) {
      return
    }
    if (isLikelySearchOrUrlField(viewNode)) {
      return
    }
    if (isTotp(viewNode)) {
      return
    }
    browserFormFieldCandidates.add(viewNode)
  }

  private fun applyBrowserFallbackCredentialFields() {
    if (!browserStrategy.allowBrowserFormFieldInference || autoFillFields.autoFillIds.isNotEmpty()) {
      return
    }
    val roles = AutofillBrowserFormFieldPolicy.inferCredentialRoles(
      browserFormFieldCandidates.mapIndexed { index, node ->
        BrowserFormFieldCandidate(
          index = index,
          top = node.top,
          isFocused = node.isFocused || node.isAccessibilityFocused,
          isPassword = isPassword(node),
          isSearchOrUrl = isLikelySearchOrUrlField(node)
        )
      }
    )
    if (roles.isEmpty()) {
      return
    }

    roles.forEach { (index, role) ->
      val node = browserFormFieldCandidates.getOrNull(index) ?: return@forEach
      when (role) {
        BrowserFormFieldRole.USERNAME -> addUserField(node, force = true)
        BrowserFormFieldRole.PASSWORD -> addPassField(node, force = true)
      }
    }
    Timber.i("browser fallback credential fields inferred, count = ${roles.size}")
  }

  private fun isLikelySearchOrUrlField(viewNode: ViewNode): Boolean {
    val tokens = ArrayList<String>()
    viewNode.idEntry?.let(tokens::add)
    viewNode.hint?.toString()?.let(tokens::add)
    viewNode.htmlInfo?.attributes?.forEach {
      if (!it.first.isNullOrEmpty()) {
        tokens.add(it.first)
      }
      if (!it.second.isNullOrEmpty()) {
        tokens.add(it.second)
      }
    }
    return tokens.any {
      browserStrategy.isSearchOrUrlFieldToken(it)
    }
  }

  /**
   * 内置浏览器
   */
  private fun innerAppWebView(viewNode: ViewNode) {
    isInnerAppW3c = true
    if (domainUrl.isBlank()) {
      AutofillBrowserUrlPolicy.normalizeDomain(viewNode.webDomain)?.let { domain ->
        domainUrl = domain
        W3cHints.curDomainUrl = domainUrl
        Timber.d("domainUrl = $domainUrl")
        ImeBrowserDomainContext.remember(
          browserPackage = pkgName,
          domain = domainUrl,
          source = ImeBrowserDomainContext.Source.WEB_DOMAIN
        )
      }
    }
    getW3CInfo(viewNode)
    val childrenSize = viewNode.childCount
    for (i in 0 until childrenSize) {
      innerAppWebView(viewNode.getChildAt(i))
    }
  }

  private fun classIsWebView(className: String?): Boolean {
    if (className.isNullOrEmpty()) return false
    if (!webViewMap.contains(className) && AutofillViewClassPolicy.isWebViewClassName(className)) {
      webViewMap.add(className)
    }
    return webViewMap.contains(className)
  }

  private fun classIsEditText(className: String?): Boolean {
    if (className.isNullOrEmpty()) return false
    if (!editTextMap.contains(className) && AutofillViewClassPolicy.isEditTextClassName(className)) {
      editTextMap.add(className)
    }
    return editTextMap.contains(className)
  }

  private fun getAndroidViewInfo(viewNode: ViewNode) {
    if (isPassword(viewNode)) {
      addPassField(viewNode)
      return
    }
    if (isTotp(viewNode)) {
      addTotpField(viewNode)
      return
    }
    if (isUserName(viewNode)) {
      addUserField(viewNode)
      return
    }
    Timber.d(
      "not w3c, unknown idEntry = ${viewNode.idEntry}, isFocused = ${viewNode.isFocused}, autofillId = ${viewNode.autofillId}, fillValue = ${viewNode.autofillValue}, inputType =  ${viewNode.inputType}, htmlInfo = ${viewNode.htmlInfo}, autofillType = ${viewNode.autofillType}, hint = ${viewNode.hint}, isAccessibilityFocused =${viewNode.isAccessibilityFocused},  idPackage = ${viewNode.idPackage}, isActivated = ${viewNode.isActivated}, visibility = ${viewNode.visibility}, isAssistBlocked = ${viewNode.isAssistBlocked}, isOpaque = ${viewNode.isOpaque}"
    )
  }

  private fun getW3CInfo(viewNode: ViewNode) {
    if (viewNode.htmlInfo == null) {
      return
    }
    if (W3cHints.isW3cTotpByHints(viewNode)) {
      Timber.i("addTotp by hints")
      addTotpField(viewNode)
      return
    }
    if (W3cHints.isW3CUserByHints(viewNode)) {
      Timber.i("addUser by hints")
      addUserField(viewNode)
      return
    }
    if (W3cHints.isW3CPassByHints(viewNode)) {
      Timber.i("addPassword by hints")
      addPassField(viewNode)
      return
    }
    Timber.d(
      "w3c, unknown idEntry = ${viewNode.idEntry}, isFocused = ${viewNode.isFocused}, autofillId = ${viewNode.autofillId}, fillValue = ${viewNode.autofillValue}, inputType =  ${viewNode.inputType}, htmlInfo = ${viewNode.htmlInfo}, autofillType = ${viewNode.autofillType}, hint = ${viewNode.hint}, isAccessibilityFocused =${viewNode.isAccessibilityFocused},  idPackage = ${viewNode.idPackage}, isActivated = ${viewNode.isActivated}, visibility = ${viewNode.visibility}, isAssistBlocked = ${viewNode.isAssistBlocked}, isOpaque = ${viewNode.isOpaque}"
    )
    val w3cAttrsDump = viewNode.htmlInfo?.attributes
      ?.joinToString(",") { "${it.first}=${it.second}" }
    if (!w3cAttrsDump.isNullOrEmpty()) {
      Timber.d("w3c, unknown attrs: autofillId=${viewNode.autofillId}, tag=${viewNode.htmlInfo?.tag}, attrs=[$w3cAttrsDump]")
    }
  }

  /**
   * Check whether the web page
   */
  private fun checkW3C(viewNode: ViewNode): Boolean {
    if (isW3c) {
      return true
    }
    isW3c = viewNode.htmlInfo?.tag == "input" || viewNode.className == "android.webkit.WebView"
    return isW3c
  }

  /**
   * add pass field
   */
  private fun addPassField(viewNode: ViewNode, force: Boolean = false) {
    if (!force && !isW3c && !isInnerAppW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
      return
    }
    autoFillFields.tempPassFillId = viewNode.autofillId
    Timber.d("pass autofillType = ${viewNode.autofillType}, fillId = ${viewNode.autofillId}, fillValue = ${viewNode.autofillValue}, text = ${viewNode.text}, hint = ${viewNode.hint}, visibility = ${viewNode.visibility}, isActivated = ${viewNode.isActivated}")
    passFields.add(viewNode)
    autoFillFields.add(AutoFillFieldMetadata(viewNode, View.AUTOFILL_HINT_PASSWORD))
  }

  private fun addTotpField(viewNode: ViewNode, force: Boolean = false) {
    if (!force && !isW3c && !isInnerAppW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
      return
    }
    Timber.d("totp autofillType = ${viewNode.autofillType}, fillId = ${viewNode.autofillId}, idEntry = ${viewNode.idEntry}, hint = ${viewNode.hint}, visibility = ${viewNode.visibility}, isActivated = ${viewNode.isActivated}")
    totpFields.add(viewNode)
    autoFillFields.add(AutoFillFieldMetadata(viewNode, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP))
  }

  /**
   * add userName field
   */
  private fun addUserField(viewNode: ViewNode, force: Boolean = false) {
    if (!force && !isW3c && !isInnerAppW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
      return
    }
    if (autoFillFields.tempUserFillId == null || viewNode.isFocused) {
      autoFillFields.tempUserFillId = viewNode.autofillId
      Timber.d("user autofillType = ${viewNode.autofillType}, fillId = ${viewNode.autofillId}, idEntry = ${viewNode.idEntry}, fillValue = ${viewNode.autofillValue} text = ${viewNode.text}, hint = ${viewNode.hint}, visibility = ${viewNode.visibility}, isActivated = ${viewNode.isActivated}")
      useFields.add(viewNode)
      autoFillFields.add(AutoFillFieldMetadata(viewNode, View.AUTOFILL_HINT_USERNAME))
    }
  }

  /**
   * 判断是否是 TOTP 输入框。
   *
   * 识别路径(按优先级):
   * 1. autocomplete/autofillHints/idEntry/hint 含 TOTP 关键字 — 适用于保留了 htmlInfo
   *    或开发者主动设置 autofillHint 的场景。
   * 2. 浏览器场景下,字段已输入 TOTP 长度(4-8 位)的纯数字内容 — 兜底信号,处理
   *    Chrome/Edge 把 HTML input 暴露成 native EditText 时 htmlInfo 全部丢失的情况。
   *    用户首次聚焦空字段时不会触发(因为此时还没有内容可判断),只有当用户输入了
   *    数字后下次 FillRequest 才会识别为 TOTP。这是有意的:不通过任何会改变原有
   *    用户名识别的兜底逻辑,只用正向证据扩展 TOTP 识别。
   * 3. 浏览器场景下,聚焦中的"无 htmlInfo + 有非空 hint + hint 不含凭证关键字"字段
   *    — 处理用户首次聚焦空 TOTP 输入框的情况。Chrome/Edge 把 HTML input 暴露成
   *    原生 EditText 时,HTML placeholder 会保留为 hint,而 URL bar 等 Chrome 内部
   *    EditText 通常 hint 为空,所以"有非空 hint"足以把 HTML input 区分出来。同时
   *    要求 hint 不含凭证关键字(账号/用户名/邮箱/密码 等),避免误吞登录页用户名框。
   */
  private fun isTotp(f: ViewNode): Boolean {
    if (isLikelySearchOrUrlField(f) || isPassword(f)) {
      return false
    }
    if (AutofillTotpFieldPolicy.isTotpField(
        autofillHints = f.autofillHints,
        idEntry = f.idEntry,
        hint = f.hint,
        htmlAttributes = f.htmlInfo?.attributes
      )
    ) {
      return true
    }
    if (browserStrategy.isBrowser && isLikelyTotpByContent(f)) {
      return true
    }
    if (browserStrategy.isBrowser && isFocusedUnmarkedBrowserInput(f)) {
      return true
    }
    return false
  }

  /**
   * 字段当前已输入的内容是否像 TOTP(纯数字、4-8 位)。
   * 用于 Chrome/Edge 丢失 htmlInfo 时通过用户已输入内容做兜底识别。
   *
   * 注意:Chrome/Edge 的虚拟 EditText 把用户输入放在 [ViewNode.getAutofillValue] 里,
   * [ViewNode.getText] 通常为空,所以必须从 autofillValue 取值。
   */
  private fun isLikelyTotpByContent(f: ViewNode): Boolean {
    val value = f.autofillValue ?: return false
    if (!value.isText) return false
    val text = value.textValue?.toString() ?: return false
    if (text.length !in 4..8) return false
    if (!text.all { it.isDigit() }) return false
    return true
  }

  /**
   * 浏览器场景下,聚焦中的"被 Chromium 剥光 htmlInfo 但保留 placeholder hint"的
   * HTML input 是否应识别为 TOTP。
   *
   * 条件:
   * - htmlInfo == null(Chromium 剥过的 native EditText 才走这条路;有 htmlInfo 的
   *   走 W3C 路径,不需要这个兜底)
   * - 字段处于聚焦或无障碍聚焦状态(用户实际在交互的输入框)
   * - hint 非空(HTML placeholder 翻译,Chrome 内部 EditText 如 URL bar 通常 hint
   *   为空,这个条件把它们排除)
   * - 字段身上没有任何凭证关键字(用户名/密码/邮箱/账号 等),否则原 isUserName /
   *   isPassword 流程已经能识别
   *
   * 这个分支是有意的保守:只在"页面把 TOTP 字段渲染为唯一可见输入框,且 placeholder
   * 与凭证无关"的常见 2FA 场景下触发。如果某些登录页用户名框的 placeholder 也不含
   * 凭证词,这个分支会误识别;那种场景下用户需要在自动填充 UI 里手动切换为用户名。
   */
  private fun isFocusedUnmarkedBrowserInput(f: ViewNode): Boolean {
    if (f.htmlInfo != null) return false
    if (!f.isFocused && !f.isAccessibilityFocused) return false
    val hint = f.hint
    if (hint.isNullOrBlank()) return false
    if (hasCredentialMarker(f)) return false
    return true
  }

  /**
   * 字段是否带任何凭证关键字(username/password/TOTP)。
   * 任意一项命中即返回 true — 表示字段已能被原有路径识别,不需要 TOTP 兜底。
   */
  private fun hasCredentialMarker(f: ViewNode): Boolean {
    if (f.autofillHints?.any { hint ->
        usernameHints.any { it.equals(hint, ignoreCase = true) } ||
          passHints.any { it.equals(hint, ignoreCase = true) }
      } == true
    ) return true
    val entry = f.idEntry
    if (!entry.isNullOrBlank() && (
        usernameHints.any { entry!!.contains(it, ignoreCase = true) } ||
          passHints.any { entry!!.contains(it, ignoreCase = true) })
    ) {
      return true
    }
    val hint = f.hint
    if (!hint.isNullOrBlank() && (
        usernameHints.any { hint!!.contains(it, ignoreCase = true) } ||
          passHints.any { hint!!.contains(it, ignoreCase = true) })
    ) {
      return true
    }
    if (AutofillTotpFieldPolicy.isTotpField(
        autofillHints = f.autofillHints,
        idEntry = f.idEntry,
        hint = f.hint,
        htmlAttributes = null
      )
    ) return true
    return false
  }

  private fun isUserName(f: ViewNode): Boolean {
    if ((f.idEntry != null && f.idEntry!!.contains("search", ignoreCase = true))
      || (f.hint != null && f.hint!!.contains("search", ignoreCase = true))
    ) {
      return false
    }
    // Defense in depth:即便 isTotp() 漏判(如 Chromium 暴露的 native EditText 没有
    // htmlInfo 导致 isTotpField 看不到 autocomplete="one-time-code"),只要字段身上
    // 还能找到任何 TOTP 标记,就绝不能回退识别为 username。
    if (AutofillTotpFieldPolicy.isTotpField(
        autofillHints = f.autofillHints,
        idEntry = f.idEntry,
        hint = f.hint,
        htmlAttributes = f.htmlInfo?.attributes
      )
    ) {
      return false
    }
    val hasUserHint = f.autofillHints?.any { hint ->
      usernameHints.any { uh -> uh.equals(hint, ignoreCase = true) }
    } == true

    if (!isPassword(f)
      || hasUserHint
      || usernameHints.any { f.idEntry != null && f.idEntry!!.contains(it, ignoreCase = true) }
      || usernameHints.any { f.hint != null && f.hint!!.contains(it, ignoreCase = true) }
    ) {

      return true
    }
    return false
  }

  /**
   * 判断是否是密码输入框
   * @return true 密码输入框
   */
  private fun isPassword(f: ViewNode): Boolean {
    val inputType = f.inputType
    if (f.idEntry?.lowercase()?.contains("search") == true
      || f.hint?.lowercase()?.contains("search") == true
    ) {
      return false
    }
    val hasPassHint = f.autofillHints?.any { hint ->
      passHints.any { ph -> ph.equals(hint, ignoreCase = true) }
    } == true
    if (inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
      || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
      || inputType == InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
      || hasPassHint
      || passHints.any { f.idEntry != null && f.idEntry!!.contains(it, ignoreCase = true) }
      || (f.autofillHints?.firstOrNull() == "passwordAuto")
    ) {
      return true
    }
    return false
  }
}
