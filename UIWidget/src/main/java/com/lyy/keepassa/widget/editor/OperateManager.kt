/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import android.text.Editable
import android.util.Log

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/11/30
 **/
class OperateManager(val ed: Editable) {

  private val TAG = javaClass.simpleName
  private val actionManager = ActionManager()

  fun destroy() {
    actionManager.getRedoQueue()
        .clear()
    actionManager.getUndoQueue()
        .clear()
  }

  fun add(
    addStr: String,
    start: Int
  ) {
    Log.d(TAG, "AddAction, content = $addStr, start = $start")
    val addAction =
      AddAction(ed, StringWrapper(content = addStr, start = start, type = TYPE_ADD))
    actionManager.setLastAction(addAction)
  }

  fun delete(
    delStr: String,
    start: Int,
    end: Int
  ) {
    Log.d(TAG, "delAction, content = $delStr, start = $start")
    val delAction =
      DeleteAction(ed, StringWrapper(content = delStr, start = start, type = TYPE_DELETE))
    actionManager.setLastAction(delAction)
  }

  fun clear(): String {
    val clearAction =
      ClearAction(ed, StringWrapper(content = ed.toString(), start = 0, type = TYPE_CLEAR))
    actionManager.setLastAction(clearAction)
    return ""
  }

  fun undo() {
    if (actionManager.canUndo()) {
      val action = actionManager.undo()
      Log.d(TAG, "undoAction, actionType = $action")
      action?.undo()
    }
    return
  }

  fun redo() {
    if (actionManager.canRedo()) {
      val action = actionManager.redo()
      Log.d(TAG, "redoAction, actionType = $action")
      action?.redo()
    }
    return
  }
}