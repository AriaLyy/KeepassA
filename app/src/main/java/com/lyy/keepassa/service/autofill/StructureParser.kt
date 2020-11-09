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
import android.util.Log
import android.view.View
import androidx.autofill.HintConstants
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadata
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadataCollection
import com.lyy.keepassa.service.autofill.model.W3cHints
import com.lyy.keepassa.util.KLog

/**
 * Parser for an AssistStructure object. This is invoked when the Autofill Service receives an
 * AssistStructure from the client Activity, representing its View hierarchy. In this sample, it
 * parses the hierarchy and collects autofill metadata from {@link ViewNode}s along the way.
 */
@TargetApi(Build.VERSION_CODES.O)
internal class StructureParser(private val autofillStructure: AssistStructure) {
  val TAG = javaClass.simpleName
  val autoFillFields = AutoFillFieldMetadataCollection()
  val useFields = ArrayList<ViewNode>()
  val passFields = ArrayList<ViewNode>()
  var domainUrl = ""
  var pkgName = ""

  // 其它应用editText 可能设置的id名，如：R.id.email
  private val usernameHints = HashSet<String>().also {
    it.add("email")
    it.add("e-email")
    it.add("account")
    it.add("user_name")
    it.add("mobile")
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY)
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH)
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR)
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
    it.add(HintConstants.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
    it.add(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
    it.add(HintConstants.AUTOFILL_HINT_PHONE)
    it.add(HintConstants.AUTOFILL_HINT_NAME)
    it.add(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS)
    it.add(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
    it.add(HintConstants.AUTOFILL_HINT_USERNAME)
  }

  private val passHints = HashSet<String>().also {
    it.add(View.AUTOFILL_HINT_PASSWORD)
    it.add("passwort")
  }

  /**
   * 是否是用户手动 用户手机选择了自动填充，也就是editText获取了焦点才开始弹出
   */
  fun parseForFill(
    isManual: Boolean,
    pkgName: String
  ) {
    this.pkgName = pkgName
    parse(isManual)
  }

  /**
   * Traverse AssistStructure and add ViewNode metadata to a flat list.
   */
  private fun parse(isManual: Boolean) {
    Log.d(TAG, "Parsing structure for " + autofillStructure.activityComponent)
    val nodes = autofillStructure.windowNodeCount
    for (i in 0 until nodes) {
      parseLocked(autofillStructure.getWindowNodeAt(i).rootViewNode)
    }
    // 如果密码为空，默认不弹出选择item，这是为了防止遇到editText就弹出item的情况
    if (passFields.isEmpty() && !isManual) {
      autoFillFields.clear()
    }
  }

  private fun parseLocked(viewNode: ViewNode) {
    // 处理editText 增加 android:autofillHints 的情况
    if (!viewNode.autofillHints.isNullOrEmpty()) {
      autoFillFields.add(AutoFillFieldMetadata(viewNode))
    } else {
      val className = viewNode.className
      if (className == "android.widget.EditText" || viewNode.htmlInfo?.tag == "input") {
        when {
          isPassword(viewNode) -> {
            Log.d(
                TAG,
                "pass autofillType = ${viewNode.autofillType}, fillId = ${viewNode.autofillId}," + " text = ${viewNode.text}, hint = ${viewNode.hint}"
            )
            passFields.add(viewNode)
            autoFillFields.add(AutoFillFieldMetadata(viewNode, View.AUTOFILL_HINT_PASSWORD))
          }
          isUserName(viewNode) -> {
            Log.d(
                TAG,
                "user idEntry = ${viewNode.idEntry}, autofillType = ${viewNode.autofillType}, " + "fillId = ${viewNode.autofillId}, text = ${viewNode.text}, hint = ${viewNode.hint}"
            )
            useFields.add(viewNode)
            autoFillFields.add(AutoFillFieldMetadata(viewNode, View.AUTOFILL_HINT_USERNAME))
          }
          else -> {
            Log.d(
                TAG,
                "unknown idEntry = ${viewNode.idEntry}, isFocused = ${viewNode.isFocused}, " + "autofillId = ${viewNode.autofillId}, inputType =  ${viewNode.inputType}, " + "htmlInfo = ${viewNode.htmlInfo}, autofillType = ${viewNode.autofillType}, " + "hint = ${viewNode.hint}, isAccessibilityFocused =${viewNode.isAccessibilityFocused}, " + "idPackage = ${viewNode.idPackage}, isActivated = ${viewNode.isActivated}, " + "visibility = ${viewNode.visibility}, isAssistBlocked = ${viewNode.isAssistBlocked}, " + "isOpaque = ${viewNode.isOpaque}"
            )
          }
        }
      }
    }

    val childrenSize = viewNode.childCount
    for (i in 0 until childrenSize) {
      parseLocked(viewNode.getChildAt(i))

    }
  }

  /**
   * 判断是否是用户名输入框
   */
  private fun isUserName(f: ViewNode): Boolean {
    if (!isPassword(f)
        || usernameHints.any { f.idEntry != null && f.idEntry.contains(it, ignoreCase = true) }
        || usernameHints.any { f.hint != null && f.hint.contains(it, ignoreCase = true) }
        || isW3cUser(f)
    ) {
      if ((f.idEntry != null && f.idEntry.contains("search", ignoreCase = false))
          || (f.hint != null && f.hint.contains("search", ignoreCase = false))
      ) {
        return false
      }
      return true
    }
    return false
  }

  private fun isW3cUser(f: ViewNode): Boolean {
    if (f.htmlInfo == null || f.htmlInfo!!.attributes == null) {
      return false
    }
    domainUrl = f.webDomain ?: ""
    return W3cHints.isW3cUserName(f)
  }

  /**
   * 判断是否是密码输入框
   * @return true 密码输入框
   */
  private fun isPassword(f: ViewNode): Boolean {
    val inputType = f.inputType
    if (inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        || inputType == InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        || isW3cPassword(f)
        || passHints.any { f.idEntry != null && f.idEntry.contains(it, ignoreCase = true) }
    ) {
      return true
    }
    return false
  }

  private fun isW3cPassword(f: ViewNode): Boolean {
    if (f.htmlInfo == null || f.htmlInfo!!.attributes == null) {
      return false
    }
    domainUrl = f.webDomain ?: ""
    return W3cHints.isW3cPassWord(f)
  }
}