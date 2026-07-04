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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

class AppSettingBrowserAutofillPreferenceTest {

  private val appNamespace = "http://schemas.android.com/apk/res-auto"

  @Test fun browserAutofillSettingsPreferenceIsBelowAutofillService() {
    val document = appSettingDocument()
    val nodes = document.getElementsByTagName("*")

    var autofillIndex = -1
    var browserAutofillIndex = -1
    for (i in 0 until nodes.length) {
      val key = nodes.item(i).attributes?.getNamedItemNS(appNamespace, "key")?.nodeValue
      if (key == "@string/set_open_auto_fill") {
        autofillIndex = i
      }
      if (key == "@string/set_key_browser_autofill_settings") {
        browserAutofillIndex = i
      }
    }

    assertTrue("Autofill service preference should be present", autofillIndex >= 0)
    assertTrue("Browser autofill preference should be present", browserAutofillIndex >= 0)
    assertTrue(browserAutofillIndex > autofillIndex)
  }

  @Test fun autofillPreferencesAreInDedicatedCategory() {
    val category = findCategoryByTitle(appSettingDocument(), "@string/auto_fill_set")

    assertNotNull("Autofill category should be present", category)
    assertTrue(category!!.directChildPreferenceKeys().contains("@string/set_open_auto_fill"))
    assertTrue(
      category.directChildPreferenceKeys().contains("@string/set_key_browser_autofill_settings")
    )
  }

  @Test fun browserAutofillSettingsUsesChromeIcon() {
    val preference = findPreferenceByKey(
      appSettingDocument(),
      "@string/set_key_browser_autofill_settings"
    )

    assertNotNull("Browser autofill preference should be present", preference)
    assertEquals(
      "@drawable/ic_chrome",
      preference!!.attributes?.getNamedItemNS(appNamespace, "icon")?.nodeValue
    )
    assertTrue(File("src/main/res/drawable/ic_chrome.xml").exists())
  }

  @Test fun appSettingFragmentInitializesBrowserAutofillSettings() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("setBrowserAutofillSettings()"))
    assertTrue(fragment.contains("ChromeAutofillSupport.openSettings"))
  }

  @Test fun browserAutofillSettingsTitleMentionsChrome() {
    val defaultTitle = stringValue(
      file = File("src/main/res/values/strings.xml"),
      name = "browser_autofill_settings_title"
    )
    val chineseTitle = stringValue(
      file = File("src/main/res/values-zh-rCN/strings.xml"),
      name = "browser_autofill_settings_title"
    )

    assertTrue(defaultTitle.contains("Chrome"))
    assertTrue(chineseTitle.contains("Chrome"))
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
