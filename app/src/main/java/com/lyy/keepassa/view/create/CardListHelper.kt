/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

import android.animation.ObjectAnimator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.Callback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.lyy.keepassa.R.id
import com.lyy.keepassa.databinding.LayoutEntryCreateStrCardBinding
import com.lyy.keepassa.util.doClick
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:27 PM 2023/10/25
 **/
internal class CardListHelper(val binding: LayoutEntryCreateStrCardBinding) {

  companion object {
    private val ARROW_ANIM_DURATION = 100L
  }

  private var isExpand = false

  fun handleList(removeCallback: () -> Unit) {
    binding.rvList.apply {
      setHasFixedSize(true)
      layoutManager = object : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean {
          return false
        }
      }
      isNestedScrollingEnabled = false
    }
    handleArrow()
    moveDrop(removeCallback)
  }

  private fun handleArrow() {
    binding.vClick.doClick {
      if (!isExpand) {
        expand()
        return@doClick
      }
      hind()
    }
  }

  private fun hind() {
    val anim = ObjectAnimator.ofFloat(binding.ivArrow, "rotation", 180f, 0f)
    anim.duration = ARROW_ANIM_DURATION
    anim.doOnEnd {
      isExpand = false
      binding.rvList.visibility = ConstraintLayout.GONE
    }
    anim.start()
  }

  /**
   * 展开列表
   */
  private fun expand() {
    val anim = ObjectAnimator.ofFloat(binding.ivArrow, "rotation", 0f, 180f)
    anim.duration = ARROW_ANIM_DURATION
    anim.doOnEnd {
      isExpand = true
      binding.rvList.visibility = ConstraintLayout.VISIBLE
    }
    anim.start()
  }

  /**
   * 滑动删除
   */
  private fun moveDrop(removeCallback: () -> Unit) {
    ItemTouchHelper(object : Callback() {
      private var swipeThreshold = -1f
      override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN //允许上下的拖动
        val swipeFlags = ItemTouchHelper.LEFT //只允许从右向左侧滑
        return makeMovementFlags(dragFlags, swipeFlags)
      }

      override fun isLongPressDragEnabled(): Boolean {
        // 禁止拖动
        return false
      }

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
      ): Boolean {
        Timber.d("onMove")
        return true
      }

      override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val delBtn = viewHolder.itemView.findViewById<TextView>(id.del)
        viewHolder.itemView.translationX = -(delBtn.measuredWidth.toFloat())
      }

      override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        if (swipeThreshold < 0) {
          val delBtn = viewHolder.itemView.findViewById<TextView>(id.del)
          swipeThreshold = delBtn.measuredWidth.toFloat() / viewHolder.itemView.measuredWidth
          removeCallback.invoke()
        }
        return swipeThreshold
      }
    }).attachToRecyclerView(binding.rvList)
  }
}