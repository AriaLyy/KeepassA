/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create.entry

/**
 * 决定 CreateEntry 保存完成后是否弹出 save_db_success toast。
 *
 * 仅当 (1) 落库真正成功 且 (2) 本次保存源自 autofill 流程
 * (autoFillParam.isSave == true) 时返回 true。
 * 普通手动新建条目场景不弹 toast,避免每次都打扰用户。
 *
 * 调用方负责把 DbSynUtil 状态码转换为 isSucceed,保持本函数纯净无 Android 依赖。
 */
internal fun shouldShowAutoFillSaveToast(isSucceed: Boolean, isAutoFillSave: Boolean): Boolean =
  isSucceed && isAutoFillSave
