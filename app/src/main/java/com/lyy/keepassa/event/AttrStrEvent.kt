/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.entity.CommonState

/**
 * 创建自定义字段事件
 */
data class AttrStrEvent(
  val state: CommonState,
  val key: String,
  val str: ProtectedString,
  val position: Int = 0
)