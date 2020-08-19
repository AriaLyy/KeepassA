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
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.example.uiwidget.R

class SimpleItemView(
  context: Context,
  attrs: AttributeSet
) : RelativeLayout(context, attrs) {
  private val icon: AppCompatImageView
  private val title: TextView
  private val des: TextView

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_simple_item, this, true)
    icon = findViewById(R.id.icon)
    title = findViewById(R.id.title)
    des = findViewById(R.id.des)
    val ta = context.obtainStyledAttributes(attrs, R.styleable.SimpleItemView)
    icon.setImageDrawable(ta.getDrawable(R.styleable.SimpleItemView_icon))
    title.text = ta.getString(R.styleable.SimpleItemView_title)
    des.text = ta.getString(R.styleable.SimpleItemView_des)

    ta.recycle()
  }
}