/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.entity

class SimpleItemEntity {
  var title: String = ""
  var subTitle: String = ""
  var content: String = ""
  var icon: Int = 0
  var id: Int = -1
  var time: Long = 0
  lateinit var obj: Any
  var isSelected: Boolean = false

  /**
   * 是否受保护
   */
  var isProtected = false
}