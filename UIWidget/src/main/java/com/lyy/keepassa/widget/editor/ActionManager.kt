/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import java.util.ArrayDeque

/**
 * @Author laoyuyu
 * @Description
 * @Date 2020/11/30
 **/
class ActionManager {
  // 最大允许撤销数
  private val capacity = 100

  // 恢复数组
  private val redoQueue = ArrayDeque<IAction>()

  // 撤销数组
  private val undoQueue = ArrayDeque<IAction>()

  fun getRedoQueue() = redoQueue

  fun getUndoQueue() = undoQueue

  fun setLastAction(action: IAction) {
    redoQueue.clear()
    undoQueue.add(action)
  }

  fun canUndo(): Boolean {
    return undoQueue.size > 0
  }

  fun canRedo(): Boolean {
    return redoQueue.size > 0
  }

  fun undo(): IAction? {
    if (redoQueue.size >= capacity) {
      redoQueue.pollFirst()
    }
    val undoAction = undoQueue.pollLast()
    redoQueue.offer(undoAction)
    return undoAction
  }

  fun redo(): IAction? {
    if (undoQueue.size >= capacity) {
      undoQueue.pollFirst()
    }
    val redoAction = redoQueue.pollLast()
    undoQueue.offer(redoAction)
    return redoAction
  }
}