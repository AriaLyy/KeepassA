/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：修复 Crashlytics issue `b0ca9aefb3287006a025b07820d9060b`。
 *
 * 根因：MIUI/HyperOS Android 16 上 [android.app.assist.AssistStructure] 加载 view 树时，
 * framework 会读 [android.provider.Settings.Secure]，calling package 校验失败抛
 * `java.lang.SecurityException: Package android does not belong to <uid>`。
 * 该异常源自系统层，无法在应用侧根治，只能吞掉以保证自动填充服务不崩溃。
 */
class AutofillParseSafelyTest {

  @Test fun normalBlock_returnsTrue() {
    val result = safeParse({ /* no-op */ }, {})
    assertTrue(result)
  }

  @Test fun securityException_returnsFalse_andCallsOnFailed() {
    var failedCalled = false

    val result = safeParse(
        parseBlock = { throw SecurityException("Package android does not belong to 10350") },
        onFailed = { failedCalled = true }
    )

    assertFalse(result)
    assertTrue(failedCalled)
  }

  @Test fun otherExceptions_propagate() {
    var threw = false
    try {
      safeParse({ throw IllegalStateException("not a SecurityException") }, {})
    } catch (e: IllegalStateException) {
      threw = true
    }
    assertTrue(threw)
  }
}
