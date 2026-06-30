/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.content.Intent
import android.view.autofill.AutofillManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * 回归测试：修复 Crashlytics issue `93e7d779a388e0f520c12d23de37be7f`。
 *
 * 根因：[android.app.assist.AssistStructure] 通过 Intent extras 在多个 Activity 间流转
 * (`AutoFillEntrySearchActivity` / `QuickUnlockActivity` / `LauncherActivity`)，
 * Activity 进入后台时 framework 会序列化 fragment state，若 Intent extras 仍持有
 * [android.app.assist.AssistStructure]，parcel 后大小可能超过 Binder 1MB 上限，抛
 * [android.os.TransactionTooLargeException]。
 *
 * 修复方式：调用方读完 [android.app.assist.AssistStructure] 后立即调
 * [Intent.clearAssistStructure] 清除 extra，避免被保存进 SavedState。
 */
class AutofillIntentCleanupTest {

  @Test fun clear_whenExtraPresent_removesIt() {
    val intent = mockk<Intent>(relaxed = true)
    every { intent.hasExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE) } returns true

    intent.clearAssistStructure()

    verify(exactly = 1) { intent.removeExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE) }
  }

  @Test fun clear_whenExtraAbsent_doesNotRemove() {
    val intent = mockk<Intent>(relaxed = true)
    every { intent.hasExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE) } returns false

    intent.clearAssistStructure()

    verify(exactly = 0) { intent.removeExtra(any()) }
  }
}
