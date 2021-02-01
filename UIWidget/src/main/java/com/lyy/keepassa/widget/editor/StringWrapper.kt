/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

/**
 * @Author laoyuyu
 * @param start the position of the string in the origin string
 * @param index the index of in the container
 * @Description
 * @Date 2021/1/15
 **/
data class StringWrapper(
  val type:Int,
  val content: String,
  val start:Int,
) {

  override fun toString(): String {
    return content
  }
}

const val TYPE_ADD = 1
const val TYPE_DELETE = 2
const val TYPE_CLEAR = 3