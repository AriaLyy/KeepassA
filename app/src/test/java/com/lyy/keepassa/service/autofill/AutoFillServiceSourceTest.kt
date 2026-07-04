/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoFillServiceSourceTest {

  @Test fun unlockedFallbackSearchPromptUsesSearchPresentationInsteadOfAuthPresentation() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/AutoFillService.kt").readText()
    val method = source.substringAfter("private fun openFallbackSearchPrompt(")
      .substringBefore("\n  /**\n   * 启动数据库验证界面或数据为空时的匹配界面")

    assertFalse(method.contains("newAuthResponse"))
    assertTrue(method.contains("newSearchResponse"))
  }

  @Test fun unlockedSearchFlowStoresBrowserMetadataBeforeOpeningSearch() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/AutoFillService.kt").readText()
    val noMatchedEntryBlock = source.substringAfter("// 没有匹配的数据，进入搜索界面")
      .substringBefore("val response =")

    assertTrue(noMatchedEntryBlock.contains("AutofillBrowserAuthContextStore.remember("))
    assertTrue(noMatchedEntryBlock.contains("metadata = autoFillFields"))
    assertTrue(noMatchedEntryBlock.indexOf("AutofillBrowserAuthContextStore.remember(") <
      noMatchedEntryBlock.indexOf("openSearchActivity("))
  }
}
