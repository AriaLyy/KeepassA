/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.app.assist.AssistStructure.ViewNode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UcBrowserAutofillCompatibilityTest {

  @Test fun ucInternationalRecognizesUcAddressBarResourceIds() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.UCMobile.intl")

    listOf(
      "address_input_search",
      "address_bar_go_search",
      "inputurl",
      "search_input",
      "search_bar",
      "search_edit_area",
      "search_copy_url",
      "search_input_scroll",
      "search_input_scroll_container"
    ).forEach { idEntry ->
      assertTrue(
        "$idEntry should be treated as UC International address bar",
        UcBrowserAutofillCompatibility.isAddressBarNode(strategy, viewNode(idEntry))
      )
    }
  }

  @Test fun ucAddressBarResourceIdsDoNotApplyToOtherBrowsers() {
    val ucHdStrategy = BrowserAutofillStrategyRegistry.forPackage("com.uc.browser.en")
    val edgeStrategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx")
    val node = viewNode("address_input_search")

    assertFalse(UcBrowserAutofillCompatibility.isAddressBarNode(ucHdStrategy, node))
    assertFalse(UcBrowserAutofillCompatibility.isAddressBarNode(edgeStrategy, node))
  }

  @Test fun ucInternationalIgnoresUnknownResourceIds() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.UCMobile.intl")

    assertFalse(
      UcBrowserAutofillCompatibility.isAddressBarNode(strategy, viewNode("login_password"))
    )
  }

  @Test fun ucInternationalRecognizesAddressBarAccessibilityDescription() {
    val strategy = BrowserAutofillStrategyRegistry.forPackage("com.UCMobile.intl")

    assertTrue(
      UcBrowserAutofillCompatibility.isAddressBarNode(
        strategy,
        viewNode(idEntry = null, contentDescription = "搜索或输入网址 编辑框")
      )
    )
  }

  @Test fun ucAddressBarAccessibilityDescriptionDoesNotApplyToOtherBrowsers() {
    val ucHdStrategy = BrowserAutofillStrategyRegistry.forPackage("com.uc.browser.en")
    val edgeStrategy = BrowserAutofillStrategyRegistry.forPackage("com.microsoft.emmx")
    val node = viewNode(idEntry = null, contentDescription = "搜索或输入网址 编辑框")

    assertFalse(UcBrowserAutofillCompatibility.isAddressBarNode(ucHdStrategy, node))
    assertFalse(UcBrowserAutofillCompatibility.isAddressBarNode(edgeStrategy, node))
  }

  private fun viewNode(
    idEntry: String?,
    contentDescription: CharSequence? = null
  ): ViewNode {
    return mockk {
      every { this@mockk.idEntry } returns idEntry
      every { this@mockk.contentDescription } returns contentDescription
    }
  }
}
