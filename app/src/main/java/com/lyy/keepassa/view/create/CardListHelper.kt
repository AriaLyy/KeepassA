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

  fun handleList() {
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
}