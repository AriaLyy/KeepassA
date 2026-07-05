/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainDialogChainOrderTest {

  @Test fun browserAutofillPromptRunsAfterAutofillPermissionPrompt() {
    val mainModule = File("src/main/java/com/lyy/keepassa/view/main/MainModule.kt")
      .readText()

    val autofillIndex = mainModule.indexOf("add(AutoFillPermissionsChain())")
    val browserIndex = mainModule.indexOf("add(BrowserAutofillPermissionsChain())")

    assertTrue("AutoFillPermissionsChain should be present", autofillIndex >= 0)
    assertTrue("BrowserAutofillPermissionsChain should be present", browserIndex >= 0)
    assertTrue(browserIndex > autofillIndex)
  }

  @Test fun browserAutofillPromptUsesCooldownInProduction() {
    val chain = File("src/main/java/com/lyy/keepassa/view/main/chain/ChromeAutofillPermissionsChain.kt")
      .readText()

    assertTrue(chain.contains("isInCooldown = cooldown.isInCooldown()"))
    assertFalse(chain.contains("isInCooldown = false"))
  }

  @Test fun browserAutofillPromptDoesNotKeepDebugInstrumentation() {
    val chain = File("src/main/java/com/lyy/keepassa/view/main/chain/ChromeAutofillPermissionsChain.kt")
      .readText()

    assertFalse(chain.contains("[DEBUG-browser-autofill]"))
  }
}
