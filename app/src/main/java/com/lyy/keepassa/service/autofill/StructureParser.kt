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
  var domainUrl = ""
  var pkgName = ""
  var isW3c = false

  companion object {
    // 其它应用editText 可能设置的id名，如：R.id.email
    val usernameHints = HashSet<String>().also {
      it.add("email")
      it.add("e-email")
      it.add("account")
      it.add("user_name")
      it.add("mobile")
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
    }
  }

  private fun clear() {
    autoFillFields.clear()
    useFields.clear()
    passFields.clear()
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
    isW3c = false
    domainUrl = ""
    Timber.d("Parsing structure for ${autofillStructure.activityComponent}")
    val nodeSize = autofillStructure.windowNodeCount
    clear()
    for (i in 0 until nodeSize) {
      parseLocked(autofillStructure.getWindowNodeAt(i).rootViewNode)
    }
    // 如果密码为空，默认不弹出选择item，这是为了防止遇到editText就弹出item的情况
    if (passFields.isEmpty() && !isManual && !isW3c) {
      autoFillFields.clear()
    }
  }

  private fun parseLocked(viewNode: ViewNode) {
    // 处理editText 增加 android:autofillHints 的情况
    if (!viewNode.autofillHints.isNullOrEmpty()) {
      if (isW3c) {
        if (W3cHints.isW3CUserByHints(viewNode)) {
          Timber.i("addUser by hints")
          addUserField(viewNode)
        } else if (W3cHints.isW3CPassByHints(viewNode)) {
          Timber.i("addPassword by hints")
          addPassField(viewNode)
        }
      } else {
        if (isPassword(viewNode)) {
          addPassField(viewNode)
        } else if (isUserName(viewNode)) {
          addUserField(viewNode)
        }
      }
      // autoFillFields.add(AutoFillFieldMetadata(viewNode))
    } else {
      val className = viewNode.className

      if (className == "android.widget.EditText") {
        when {
          isPassword(viewNode) -> {
            addPassField(viewNode)
          }
          isUserName(viewNode) -> {
            addUserField(viewNode)
          }
          else -> {
            Timber.d(
              "unknown idEntry = ${viewNode.idEntry}, isFocused = ${viewNode.isFocused}, autofillId = ${viewNode.autofillId}, fillValue = ${viewNode.autofillValue}, inputType =  ${viewNode.inputType}, htmlInfo = ${viewNode.htmlInfo}, autofillType = ${viewNode.autofillType}, hint = ${viewNode.hint}, isAccessibilityFocused =${viewNode.isAccessibilityFocused},  idPackage = ${viewNode.idPackage}, isActivated = ${viewNode.isActivated}, visibility = ${viewNode.visibility}, isAssistBlocked = ${viewNode.isAssistBlocked}, isOpaque = ${viewNode.isOpaque}"
            )
          }
        }
      }

      if (W3cHints.isBrowser(pkgName)) {
        Timber.i("is browser, start get web info")
        checkW3C(viewNode)
        if (isW3c) {
          if (domainUrl.isBlank()) {
            domainUrl = viewNode.webDomain ?: ""
            Timber.d("webDomain = $domainUrl")
          }
          getW3CInfo(viewNode)
        }
      }
    }

    val childrenSize = viewNode.childCount
    for (i in 0 until childrenSize) {
      parseLocked(viewNode.getChildAt(i))
    }
  }

  private fun getW3CInfo(viewNode: ViewNode) {
    viewNode.htmlInfo?.let {
      if (it.attributes != null) {
        it.attributes!!.forEach { attr ->
          if (W3cHints.isW3cPassWord(attr)) {
            addPassField(viewNode)
            return@forEach
          }
          if (W3cHints.isW3cUserName(attr)) {
            addUserField(viewNode)
          }
        }
      }
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
  private fun addPassField(viewNode: ViewNode) {
    if (!isW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
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
  private fun addUserField(viewNode: ViewNode) {
    if (!isW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
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
    if (!isPassword(f)
      || usernameHints.any { f.idEntry != null && f.idEntry!!.contains(it, ignoreCase = true) }
      || usernameHints.any { f.hint != null && f.hint!!.contains(it, ignoreCase = true) }
    ) {
      if ((f.idEntry != null && f.idEntry!!.contains("search", ignoreCase = false))
        || (f.hint != null && f.hint!!.contains("search", ignoreCase = false))
      ) {
        return false
      }
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
    if (inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
      || inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
      || inputType == InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
      || passHints.any { f.idEntry != null && f.idEntry!!.contains(it, ignoreCase = true) }
    ) {
      return true
    }
    return false
  }
}