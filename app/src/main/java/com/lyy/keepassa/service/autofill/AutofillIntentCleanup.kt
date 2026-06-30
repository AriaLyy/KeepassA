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

/**
 * 从 Intent extras 中移除 [android.app.assist.AssistStructure]。
 *
 * 修复 Crashlytics issue `93e7d779a388e0f520c12d23de37be7f`:
 * 多个 Activity 通过 Intent extras 传递 [android.app.assist.AssistStructure]，导致 onStop
 * 保存状态时 fragment state parcel 超过 Binder 1MB 上限，抛 TransactionTooLargeException。
 *
 * 调用约定：调用方先用 `Intent.getParcelableExtra` 读取结构数据，读完立即调用本扩展清除，
 * 避免 framework 在 onSaveInstanceState 时序列化大对象。
 */
internal fun Intent.clearAssistStructure() {
  if (hasExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)) {
    removeExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
  }
}
