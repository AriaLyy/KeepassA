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
  private val redos = ArrayDeque<IAction>()

  // 撤销数组
  private val undos = ArrayDeque<IAction>()

  fun setLastAction(action: IAction) {
    redos.clear()
    undos.add(action)
  }

  fun canUndo(): Boolean {
    return undos.size > 0
  }

  fun canRedo(): Boolean {
    return redos.size > 0
  }

  fun undo(): IAction? {
    if (redos.size >= capacity) {
      redos.pollFirst()
    }
    val undoAction = undos.pollLast()
    redos.offer(undoAction)
    return undoAction
  }

  fun redo(): IAction? {
    if (undos.size >= capacity) {
      undos.pollFirst()
    }
    val redoAction = redos.pollLast()
    undos.offer(redoAction)
    return redoAction
  }
}