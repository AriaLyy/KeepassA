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
 * 在加载 AssistStructure 时抛出的 SecurityException，避免崩溃。
 *
 * @param parseBlock 实际 parse 逻辑
 * @param onFailed parse 失败时的清场回调（如清空已收集字段）
 * @return true 表示 parse 正常结束；false 表示被 SecurityException 中断
 */
internal inline fun safeParse(parseBlock: () -> Unit, onFailed: () -> Unit): Boolean {
  return try {
    parseBlock()
    true
  } catch (e: SecurityException) {
    Timber.e(e, "AssistStructure parse blocked by OEM SecurityException")
    onFailed()
    false
  }
}
