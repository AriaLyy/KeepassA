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

  @Test fun supportedBrowsersDialogUsesMsgDialogContainer() {
    val source = File("src/main/java/com/lyy/keepassa/view/setting/SupportedBrowsersDialog.kt")
      .readText()

    assertFalse(source.contains("AlertDialog.Builder"))
    assertTrue(source.contains("Routerfit.create(DialogRouter::class.java).showMsgDialog"))
    assertTrue(source.contains("msgTitle = context.getString("))
    assertTrue(source.contains("msgContent = buildMessage(context, browsers)"))
    assertTrue(source.contains("showCancelBt = false"))
  }
}
