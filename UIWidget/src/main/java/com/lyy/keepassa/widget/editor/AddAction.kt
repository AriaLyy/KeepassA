/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import android.text.Editable

/**
 * @Author laoyuyu
 * @param start the chart index
 * @Description
 * @Date 2020/11/30
 **/
data class AddAction(
  val ed: Editable,
  val addStr: StringWrapper
) : IAction {

  override fun redo(): Boolean {
    ed.insert(addStr.start, addStr.content)
    return true
  }

  override fun undo(): Boolean {
    ed.replace((addStr.start - addStr.content.length) + 1, addStr.start + 1, "")
    return true
  }
}