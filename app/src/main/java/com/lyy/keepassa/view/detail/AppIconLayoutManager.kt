/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutParams

class AppIconLayoutManager(private val offset: Int) : LayoutManager() {

  override fun generateDefaultLayoutParams(): LayoutParams {
    return LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.MATCH_PARENT
    )
  }

  override fun onMeasure(
    recycler: RecyclerView.Recycler,
    state: RecyclerView.State,
    widthSpec: Int,
    heightSpec: Int
  ) {
    if (state.itemCount == 0) {
      super.onMeasure(recycler, state, widthSpec, heightSpec)
      return
    }
    if (state.isPreLayout) return

    //假定每个item的宽高一直，所以用第一个view计算宽高，
    //这种方式可能不太好
    val itemView = recycler.getViewForPosition(0)
    addView(itemView)
    //这里不能用measureChild方法，具体看内部源码实现，内部getWidth默认为0
//        measureChildWithMargins(itemView, 0, 0)
    itemView.measure(widthSpec, heightSpec)
    val mItemWidth = getDecoratedMeasuredWidth(itemView)
    val mItemHeight = getDecoratedMeasuredHeight(itemView)
    //回收这个View
    detachAndScrapView(itemView, recycler)

    //设置宽高
    setMeasuredDimension(mItemWidth, mItemHeight)
  }

  override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {

    //轻量级的将view移除屏幕
    detachAndScrapAttachedViews(recycler)
    //开始填充view

    var totalSpace = width - paddingRight
    var currentPosition = 0

    var left = 0
    val top = 0
    var right = 0
    var bottom = 0
    //模仿LinearLayoutManager的写法，当可用距离足够和要填充
    //的itemView的position在合法范围内才填充View
    while (totalSpace > 0 && currentPosition < state.itemCount) {
      val view = recycler.getViewForPosition(currentPosition)
      addView(view)
      measureChild(view, 0, 0)

      right = left + getDecoratedMeasuredWidth(view)
      bottom = top + getDecoratedMeasuredHeight(view)
      layoutDecorated(view, left, top, right, bottom)

      currentPosition++
      left += getDecoratedMeasuredWidth(view) - offset
      //关键点
      totalSpace -= getDecoratedMeasuredWidth(view) - offset
    }
    //layout完成后输出相关信息
  }
}