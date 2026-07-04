/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create.entry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试:修复 AutoFillService.onSaveRequest 提前弹出 save_db_success toast 的问题。
 *
 * 决策:仅当 (1) 落库真正成功 且 (2) 本次保存源自 autofill 流程
 * (autoFillParam.isSave == true) 时,才在 CreateEntry 完成保存后弹一次成功 toast。
 * 其他场景(普通新建条目、保存失败)均不弹。
 */
class AutoFillSaveToastDecisionTest {

  @Test fun succeedAndAutoFill_returnsTrue() {
    assertTrue(shouldShowAutoFillSaveToast(isSucceed = true, isAutoFillSave = true))
  }

  @Test fun failedAndAutoFill_returnsFalse() {
    assertFalse(shouldShowAutoFillSaveToast(isSucceed = false, isAutoFillSave = true))
  }

  @Test fun succeedAndNotAutoFill_returnsFalse() {
    assertFalse(shouldShowAutoFillSaveToast(isSucceed = true, isAutoFillSave = false))
  }

  @Test fun failedAndNotAutoFill_returnsFalse() {
    assertFalse(shouldShowAutoFillSaveToast(isSucceed = false, isAutoFillSave = false))
  }
}
