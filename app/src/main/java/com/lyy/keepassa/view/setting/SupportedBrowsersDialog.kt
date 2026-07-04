/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.lyy.keepassa.R
import com.lyy.keepassa.service.autofill.BrowserAutofillEngine
import com.lyy.keepassa.service.autofill.BrowserAutofillStrategyRegistry
import com.lyy.keepassa.service.autofill.SupportedBrowser

/**
 * 已适配浏览器列表弹窗。按引擎分组、组内按展示名排序展示。
 */
object SupportedBrowsersDialog {

  private val engineOrder: List<BrowserAutofillEngine> = listOf(
    BrowserAutofillEngine.CHROMIUM,
    BrowserAutofillEngine.YANDEX,
    BrowserAutofillEngine.KIWI,
    BrowserAutofillEngine.IDM,
    BrowserAutofillEngine.UC,
    BrowserAutofillEngine.GECKO,
    BrowserAutofillEngine.ANDROID_BROWSER,
    BrowserAutofillEngine.DEFAULT,
  )

  fun show(context: Context) {
    val browsers = BrowserAutofillStrategyRegistry.supportedBrowsers
    val message = buildMessage(context, browsers)
    AlertDialog.Builder(context)
      .setTitle(context.getString(R.string.supported_browsers_dialog_title, browsers.size))
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun buildMessage(context: Context, browsers: List<SupportedBrowser>): CharSequence {
    val sb = StringBuilder()
    val incompatibleSuffix = context.getString(R.string.supported_browser_incompatible_suffix)
    engineOrder.forEach { engine ->
      val inGroup = browsers.filter { it.engine == engine }
      if (inGroup.isEmpty()) return@forEach
      if (sb.isNotEmpty()) sb.append("\n\n")
      sb.append(categoryTitle(context, engine)).append('\n')
      inGroup
        .sortedBy { it.displayName.lowercase() }
        .forEach { browser ->
          sb.append("· ").append(browser.displayName)
          if (!browser.compatible) {
            sb.append(' ').append(incompatibleSuffix)
          }
          sb.append('\n')
        }
    }
    return sb.toString().trimEnd('\n')
  }

  private fun categoryTitle(context: Context, engine: BrowserAutofillEngine): String {
    val res = when (engine) {
      BrowserAutofillEngine.CHROMIUM -> R.string.autofill_browser_category_chromium
      BrowserAutofillEngine.YANDEX -> R.string.autofill_browser_category_yandex
      BrowserAutofillEngine.KIWI -> R.string.autofill_browser_category_kiwi
      BrowserAutofillEngine.IDM -> R.string.autofill_browser_category_idm
      BrowserAutofillEngine.UC -> R.string.autofill_browser_category_uc
      BrowserAutofillEngine.GECKO -> R.string.autofill_browser_category_gecko
      BrowserAutofillEngine.ANDROID_BROWSER -> R.string.autofill_browser_category_android
      BrowserAutofillEngine.DEFAULT -> R.string.autofill_browser_category_default
    }
    return context.getString(res)
  }
}
