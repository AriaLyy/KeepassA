/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import androidx.core.widget.NestedScrollView

class MaxHeightNestedScrollView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

  private val maxHeight: Int

  init {
    val typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.maxHeight))
    maxHeight = typedArray.getDimensionPixelSize(0, 0)
    typedArray.recycle()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val finalHeightSpec = if (maxHeight > 0) {
      MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
    } else {
      heightMeasureSpec
    }
    super.onMeasure(widthMeasureSpec, finalHeightSpec)
  }
}
