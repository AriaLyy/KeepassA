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
 * @Description
 * @Date 2020/11/30
 **/
class DeleteAction(
  val container: MutableList<CharSequence>,
  val delStr: CharSequence
) : IAction {
  override fun execute(): Boolean {
    return container.remove(delStr)
  }

  override fun redo(): Boolean {
    return container.remove(delStr)
  }

  override fun undo(): Boolean {
    return container.add(delStr)
  }
}