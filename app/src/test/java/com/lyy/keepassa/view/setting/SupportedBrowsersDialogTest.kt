/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedBrowsersDialogTest {

  @Test fun supportedBrowsersDialogUsesDedicatedDialogContainer() {
    val source = File("src/main/java/com/lyy/keepassa/view/setting/SupportedBrowsersDialog.kt")
      .readText()

    assertFalse(source.contains("AlertDialog.Builder"))
    assertFalse(source.contains("showMsgDialog"))
    assertFalse(source.contains("fillDialogContent"))
    assertTrue(source.contains("Routerfit.create(DialogRouter::class.java).showSupportedBrowsersDialog()"))
  }

  @Test fun browserDialogDoesNotModifyGenericMsgDialog() {
    val router = File("src/main/java/com/lyy/keepassa/router/DialogRouter.kt").readText()
    val dialog = File("src/main/java/com/lyy/keepassa/view/dialog/MsgDialog.kt").readText()
    val layout = File("src/main/res/layout/dialog_msg.xml").readText()
    val scrollView = File("src/main/java/com/lyy/keepassa/widget/MaxHeightNestedScrollView.kt")
      .readText()

    assertFalse(router.contains("fillDialogContent"))
    assertFalse(dialog.contains("fillDialogContent"))
    assertFalse(dialog.contains("msgContentView"))
    assertFalse(dialog.contains("buttonFlow"))
    assertFalse(layout.contains("msgContentView"))
    assertFalse(layout.contains("buttonFlow"))
    assertFalse(layout.contains("android:fillViewport=\"true\""))
    assertTrue(scrollView.contains("private val maxHeight: Int"))
  }

  @Test fun dedicatedSupportedBrowsersDialogOwnsExpandedScrollableLayout() {
    val router = File("src/main/java/com/lyy/keepassa/router/DialogRouter.kt").readText()
    val dialog = File("src/main/java/com/lyy/keepassa/view/setting/SupportedBrowsersListDialog.kt")
      .readText()
    val layout = File("src/main/res/layout/dialog_supported_browsers.xml").readText()

    assertTrue(router.contains("fun showSupportedBrowsersDialog()"))
    assertTrue(dialog.contains("@Route(path = \"/dialog/supportedBrowsers\")"))
    assertTrue(dialog.contains("R.layout.dialog_supported_browsers"))
    assertTrue(dialog.contains("BrowserAutofillStrategyRegistry.supportedBrowsers"))
    assertTrue(dialog.contains("dialog?.window?.setLayout("))
    assertTrue(layout.contains("android:id=\"@+id/supportedBrowserScroll\""))
    assertTrue(layout.contains("android:id=\"@+id/supportedBrowserContent\""))
    assertTrue(layout.contains("android:fillViewport=\"true\""))
    assertTrue(layout.contains("android:scrollbarStyle=\"outsideOverlay\""))
    assertTrue(layout.contains("app:layout_constraintEnd_toEndOf=\"parent\""))
  }
}
