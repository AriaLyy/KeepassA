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

class AutofillAuthenticationActivitySourceTest {

  @Test fun launcherRefreshesIntentBeforeReadingAutofillExtras() {
    val source = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt").readText()
    val method = source.substringAfter("override fun onNewIntent(intent: Intent?)")
      .substringBefore("\n  private fun getAutoFillParam()")

    assertTrue(method.contains("setIntent(intent)"))
    assertTrue(method.indexOf("setIntent(intent)") < method.indexOf("getAutoFillParam()"))
  }

  @Test fun autofillAuthenticationActivitiesDoNotStartNewTaskBeforeReturningResult() {
    val launcherSource = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt").readText()
    val quickUnlockSource = File("src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt").readText()
    val searchSource = File("src/main/java/com/lyy/keepassa/view/search/AutoFillEntrySearchActivity.kt").readText()

    val launcherAuthMethod = launcherSource.substringAfter("internal fun getAuthDbIntentSender(")
      .substringBefore("\n    /**\n     * 数据库未解锁，保存数据时打开数据库，并保存")
    val quickUnlockAuthMethod = quickUnlockSource.substringAfter("internal fun getQuickUnlockSenderForResponse(")
      .substringBefore("\n  override fun onDestroy()")
    val searchPendingMethod = searchSource.substringAfter("internal fun createSearchPending(")
      .substringBefore("\n\n    /**\n     * 没有匹配数据时，启动搜索界面")
    val searchIntentSenderMethod = searchSource.substringAfter("internal fun getSearchIntentSender(")
      .substringBefore("\n  }\n\n  override fun setLayoutId()")

    listOf(
      launcherAuthMethod,
      quickUnlockAuthMethod,
      searchPendingMethod,
      searchIntentSenderMethod
    ).forEach { method ->
      assertFalse(method.contains("Intent.FLAG_ACTIVITY_NEW_TASK"))
      assertFalse(method.contains("Intent.FLAG_ACTIVITY_CLEAR_TASK"))
    }
  }
}
