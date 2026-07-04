/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import timber.log.Timber

/**
 * 包裹自动填充 [StructureParser.parse] 调用，吞掉 OEM ROM（如 MIUI/HyperOS Android 16+）
 * 在加载 AssistStructure 时抛出的异常，避免崩溃。
 *
 * MIUI/HyperOS 在 `AssistStructure.ensureData` 跨 binder 拉取字段时，会触发
 * `ActivityThreadImpl.isMiuiConsumeForAutofill` → `Settings.Secure` 读取；该读取在
 * 跨进程场景下被 AppOps `enforceSettingReadable` 拒绝，本地侧 Parcel 层抛出的异常
 * 类型在不同 Android 版本上不固定（SecurityException / RuntimeException /
 * IllegalStateException / DeadObjectException 包装类都观测到过），这里统一兜底。
 *
 * 注：故意不用 `inline`。inline 的 try/catch 在某些 R8/lambda 组合下会被改写，
 * 实测中无法稳定兜住跨 binder 的异常;保留普通函数调用边界更可靠。
 *
 * @param parseBlock 实际 parse 逻辑
 * @param onFailed parse 失败时的清场回调（如清空已收集字段）
 * @return true 表示 parse 正常结束；false 表示被异常中断
 */
internal fun safeParse(parseBlock: () -> Unit, onFailed: () -> Unit): Boolean {
  return try {
    parseBlock()
    true
  } catch (e: SecurityException) {
    Timber.e(e, "AssistStructure parse blocked by OEM SecurityException")
    onFailed()
    false
  } catch (e: RuntimeException) {
    Timber.e(e, "AssistStructure parse blocked by runtime exception (Parcel/AppOps)")
    onFailed()
    false
  }
}
