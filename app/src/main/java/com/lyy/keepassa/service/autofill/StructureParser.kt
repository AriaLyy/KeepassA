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
    }

    val passHints = HashSet<String>().also {
      it.add(HintConstants.AUTOFILL_HINT_PASSWORD)
      it.add(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
      it.add("passwort")
      it.add("passwordAuto")
      it.add("pswd")
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
    // 如果密码为空，默认不弹出选择item，这是为了防止遇到editText就弹出item的情况
    if (passFields.isEmpty() && !isManual && !isW3c) {
      autoFillFields.clear()
    }
  }

  private fun parseLocked(viewNode: ViewNode) {
    // 尽早捕获 domainUrl,避免 AutoFillService 因 domainUrl 为空回退到按包名匹配(浏览器场景下匹配错误)
    if (domainUrl.isBlank() && !viewNode.webDomain.isNullOrEmpty()) {
      domainUrl = viewNode.webDomain!!
      W3cHints.curDomainUrl = domainUrl
      Timber.d("domainUrl = $domainUrl")
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
      domainUrl = viewNode.webDomain ?: ""
      W3cHints.curDomainUrl = domainUrl
      Timber.d("domainUrl = $domainUrl")
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
   * 判断是否是用户名输入框
   */
  private fun isUserName(f: ViewNode): Boolean {
    if ((f.idEntry != null && f.idEntry!!.contains("search", ignoreCase = true))
      || (f.hint != null && f.hint!!.contains("search", ignoreCase = true))
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
