/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

import com.keepassdroid.database.PwGroup

/**
 * 创建群组的事件
 */
data class CreateOrUpdateGroupEvent(
  val pwGroup: PwGroup,
  val isUpdate: Boolean = false
)