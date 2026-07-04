/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.util

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualDbLockEntryPointTest {

  @Test fun mainSettingManualLockUsesCommonLockEntryPoint() {
    val source = File("src/main/java/com/lyy/keepassa/view/main/MainSettingActivity.kt")
      .readText()
    val changeDbClickBlock = source
      .substringAfter("R.id.change_db -> {")
      .substringBefore("\n      }")

    assertTrue(
      "Manual database lock must use KeepassAUtil.lockDb(DbLockTrigger.MANUAL_LOCK)",
      changeDbClickBlock.contains("KeepassAUtil.instance.lockDb(DbLockTrigger.MANUAL_LOCK)")
    )
    assertFalse(
      "Manual database lock must not bypass lock planner by calling turnLauncher() directly",
      changeDbClickBlock.contains("turnLauncher(")
    )
  }

  @Test fun keepassAUtilKeepsLockAsCompatibilityWrapper() {
    val source = File("src/main/java/com/lyy/keepassa/util/KeepassAUtil.kt")
      .readText()
    val lockFunction = source
      .substringAfter("fun lock()")
      .substringBefore("\n  /**")

    assertTrue(source.contains("fun lockDb(trigger: DbLockTrigger"))
    assertTrue(lockFunction.contains("lockDb(DbLockTrigger.AUTO_LOCK)"))
  }

  @Test fun floatingMenuManualLockUsesCommonLockEntryPointWithoutExtraFinish() {
    val source = File("src/main/java/com/lyy/keepassa/base/BaseActivity.kt")
      .readText()
    val showQuickUnlockDialogFunction = source
      .substringAfter("protected fun showQuickUnlockDialog() {")
      .substringBefore("\n  }")

    assertTrue(
      "Manual floating-menu lock must use KeepassAUtil.lockDb(DbLockTrigger.MANUAL_LOCK)",
      showQuickUnlockDialogFunction.contains("KeepassAUtil.instance.lockDb(DbLockTrigger.MANUAL_LOCK)")
    )
    assertFalse(
      "Manual floating-menu lock must not add finish behavior outside DbLockPlanner",
      showQuickUnlockDialogFunction.contains("finish()")
    )
    assertFalse(
      "Manual floating-menu lock must not bypass trigger-aware lockDb",
      showQuickUnlockDialogFunction.contains("KeepassAUtil.instance.lock()")
    )
  }
}
