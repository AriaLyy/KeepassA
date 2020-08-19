/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

/**
 * 时间事件
 */
data class TimeEvent(
  val year: Int,
  val month: Int,
  val dayOfMonth: Int,
  val hour: Int,
  val minute: Int
)