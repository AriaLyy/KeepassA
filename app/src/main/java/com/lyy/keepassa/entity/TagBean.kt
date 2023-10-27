/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.entity

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:45 PM 2023/10/26
 **/
data class TagBean(
  val tag: String,
  var isSet: Boolean = false
) : java.io.Serializable