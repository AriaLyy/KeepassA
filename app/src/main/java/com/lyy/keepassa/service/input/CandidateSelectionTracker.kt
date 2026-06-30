/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

/**
 * 候选条目选择状态机。
 *
 * 收敛 InputIMEService 原先散落在三处的状态(curEntry、SimpleItemEntity.isSelected、
 * OnItemClickListener.lastPosition),避免 showEntryList 重复调用覆盖用户选择。
 *
 * 不变式:
 * - [entries] 非空时,[selectedIndex] ∈ [0, entries.size);否则为 -1。
 * - [selectedFlags] 与 [selectedIndex] 一致,Adapter 可直接绑定。
 */
class CandidateSelectionTracker<T> {

  private var entries: List<T> = emptyList()
  private var selectedIndex: Int = -1

  val isEmpty: Boolean get() = entries.isEmpty()
  val size: Int get() = entries.size

  /** 当前选中的条目;无候选时为 null。 */
  val current: T?
    get() = if (selectedIndex in entries.indices) entries[selectedIndex] else null

  /** 与 [entries] 等长的选中标记,供 Adapter 直接绑定到 itemView.isSelected。 */
  fun selectedFlags(): List<Boolean> = List(entries.size) { it == selectedIndex }

  /** 按位置取候选;越界返回 null。 */
  fun entryAt(position: Int): T? =
    if (position in entries.indices) entries[position] else null

  /** 该位置是否为当前选中位。 */
  fun isSelected(position: Int): Boolean = position == selectedIndex

  /**
   * 重建候选列表。新字段(输入框切换)必须走这里,以保证选中态重置。
   *
   * - 空:[current] = null
   * - 单元素:直接选中
   * - 多元素:默认选第一个
   */
  fun show(entries: List<T>) {
    if (entries.isEmpty()) {
      this.entries = emptyList()
      this.selectedIndex = -1
      return
    }
    this.entries = entries.toList()
    this.selectedIndex = 0
  }

  /**
   * 用同形状的新列表替换,但若与上次元素一致(按引用),保留 [selectedIndex]。
   *
   * 用于"用户已选过 B,按按钮时又搜了一次同样的列表"场景,避免覆盖用户选择。
   *
   * @return true 保留了之前的选中;false 列表变了或之前无选中(已 fallback 到 [show])。
   */
  fun resync(newEntries: List<T>): Boolean {
    if (newEntries.isEmpty()) {
      show(emptyList())
      return false
    }
    val sameShape = newEntries.size == entries.size &&
        newEntries.indices.all { newEntries[it] === entries[it] }
    if (sameShape && selectedIndex in newEntries.indices) {
      entries = newEntries.toList()
      return true
    }
    show(newEntries)
    return false
  }

  /**
   * 用户点击候选项。
   * @return true 选中发生变化(需要刷新 Adapter);false 同位置或越界(无需刷新)。
   */
  fun click(position: Int): Boolean {
    if (position !in entries.indices) return false
    if (position == selectedIndex) return false
    selectedIndex = position
    return true
  }
}
