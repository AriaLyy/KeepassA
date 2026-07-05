/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

class AppSettingBrowserAutofillPreferenceTest {

  private val appNamespace = "http://schemas.android.com/apk/res-auto"

  @Test fun browserAutofillSettingsPreferenceIsBelowAutofillService() {
    val category = findCategoryByTitle(appSettingDocument(), "@string/auto_fill_set")

    assertNotNull("Autofill category should be present", category)
    assertEquals(
      "@string/set_key_auto_fill_category",
      category!!.attributes?.getNamedItemNS(appNamespace, "key")?.nodeValue
    )
  }

  @Test fun autofillPreferencesAreInDedicatedCategory() {
    val category = findCategoryByTitle(appSettingDocument(), "@string/auto_fill_set")

    assertNotNull("Autofill category should be present", category)
    assertTrue(category!!.directChildPreferenceKeys().contains("@string/set_open_auto_fill"))
    assertTrue(category.directChildPreferenceKeys().contains("@string/set_key_supported_browsers"))
  }

  @Test fun browserAutofillSettingsAreCreatedDynamically() {
    val document = appSettingDocument()

    assertEquals(
      null,
      findPreferenceByKey(document, "@string/set_key_browser_autofill_settings")
    )
    assertTrue(File("src/main/res/drawable/ic_chrome.xml").exists())
  }

  @Test fun browserAutofillSettingsAreOrderedBelowSupportedBrowsersItem() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("private const val SUPPORTED_BROWSERS_ORDER = 90"))
    assertTrue(
      fragment.contains(
        "private const val BROWSER_AUTOFILL_SETTINGS_ORDER_START = SUPPORTED_BROWSERS_ORDER + 1"
      )
    )
    assertTrue(fragment.contains("preference.order = SUPPORTED_BROWSERS_ORDER"))
  }

  @Test fun browserAutofillIconsAreBoundTo24dp() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("browserAutofillIconSizePx()"))
    assertTrue(fragment.contains("24.toPx()"))
    assertTrue(
      fragment.contains(
        "icon.setBounds(0, 0, browserAutofillIconSizePx(), browserAutofillIconSizePx())"
      )
    )
  }

  @Test fun chromeAutofillIconUsesRealChromeColors() {
    val icon = File("src/main/res/drawable/ic_chrome.xml").readText()

    assertTrue(icon.contains("android:width=\"24dp\""))
    assertTrue(icon.contains("android:height=\"24dp\""))
    assertFalse(icon.contains("@color/color_icon_grey"))
    assertTrue(icon.contains("#EA4335"))
    assertTrue(icon.contains("#FBBC05"))
    assertTrue(icon.contains("#34A853"))
    assertTrue(icon.contains("#4285F4"))
  }

  @Test fun appSettingFragmentInitializesBrowserAutofillSettings() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("setBrowserAutofillSettings()"))
    assertTrue(fragment.contains("BrowserThirdPartyAutofillSupport.integrations"))
    assertTrue(fragment.contains("BrowserThirdPartyAutofillSupport.openSettings"))
    assertFalse(fragment.contains("ChromeAutofillSupport.openSettings"))
  }

  @Test fun autofillServiceSwitchUsesUnifiedServiceStatus() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("KeepassAutofillServiceStatus.isEnabled"))
    assertFalse(fragment.contains("autoFill.isChecked = am.hasEnabledAutofillServices()"))
  }

  @Test fun browserAutofillSettingsTitleUsesBrowserNamePlaceholder() {
    val defaultTitle = stringValue(
      file = File("src/main/res/values/strings.xml"),
      name = "browser_third_party_autofill_settings_title"
    )
    val chineseTitle = stringValue(
      file = File("src/main/res/values-zh-rCN/strings.xml"),
      name = "browser_third_party_autofill_settings_title"
    )

    assertTrue(defaultTitle.contains("%1\$s"))
    assertTrue(chineseTitle.contains("%1\$s"))
  }

  private fun stringValue(file: File, name: String): String {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val nodes = document.getElementsByTagName("string")
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.attributes?.getNamedItem("name")?.nodeValue == name) {
        return node.textContent
      }
    }
    throw AssertionError("Missing string resource: $name")
  }

  private fun appSettingDocument(): Document {
    return DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder()
      .parse(File("src/main/res/xml/app_setting.xml"))
  }

  private fun findCategoryByTitle(document: Document, title: String): Node? {
    val nodes = document.getElementsByTagName("PreferenceCategory")
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.attributes?.getNamedItemNS(appNamespace, "title")?.nodeValue == title) {
        return node
      }
    }
    return null
  }

  private fun findPreferenceByKey(document: Document, key: String): Node? {
    val nodes = document.getElementsByTagName("*")
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.attributes?.getNamedItemNS(appNamespace, "key")?.nodeValue == key) {
        return node
      }
    }
    return null
  }

  private fun Node.directChildPreferenceKeys(): List<String> {
    val keys = mutableListOf<String>()
    val childNodes = childNodes
    for (i in 0 until childNodes.length) {
      val child = childNodes.item(i)
      if (child.nodeType == Node.ELEMENT_NODE) {
        child.attributes?.getNamedItemNS(appNamespace, "key")?.nodeValue?.let(keys::add)
      }
    }
    return keys
  }
}
