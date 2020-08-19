/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

/**
 * msg dialog 事件
 */
data class MsgDialogEvent(
  /**
   * @param type 1、确认，2、覆盖、3、取消
   *
   */
  val type: Int = 1,
  val requestCode: Int = 0   // 请求码
)