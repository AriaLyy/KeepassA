/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import java.lang.StringBuilder

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/11/30
 **/
class OperateManager {

  private val container = mutableListOf<CharSequence>()
  private val actionManager = ActionManager()

  fun add(addStr: CharSequence) {
    val addAction = AddAction(container, addStr)
    addAction.execute()
    actionManager.setLastAction(addAction)
  }

  fun delete(delStr: CharSequence) {
    val delAction = DeleteAction(container, delStr)
    delAction.execute()
    actionManager.setLastAction(delAction)
  }

  fun clear(): String {
    val clearAction = ClearAction(container)
    clearAction.execute()
    actionManager.setLastAction(clearAction)
    return ""
  }

  fun undo(): String {
    if (actionManager.canUndo()) {
      actionManager.undo()?.undo()
    }
    return containerToString()
  }

  fun redo(): String {
    if (actionManager.canRedo()) {
      actionManager.redo()
          ?.redo()
    }
    return containerToString()
  }

  private fun containerToString(): String {
    val sb = StringBuilder()
    container.forEach {
      sb.append(it)
    }
    return sb.toString()
  }

}