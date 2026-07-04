/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MainDialogChainOrderTest {

  @Test fun chromeAutofillPromptRunsAfterAutofillPermissionPrompt() {
    val mainModule = File("src/main/java/com/lyy/keepassa/view/main/MainModule.kt")
      .readText()

    val autofillIndex = mainModule.indexOf("add(AutoFillPermissionsChain())")
    val chromeIndex = mainModule.indexOf("add(ChromeAutofillPermissionsChain())")

    assertTrue("AutoFillPermissionsChain should be present", autofillIndex >= 0)
    assertTrue("ChromeAutofillPermissionsChain should be present", chromeIndex >= 0)
    assertTrue(chromeIndex > autofillIndex)
  }
}
