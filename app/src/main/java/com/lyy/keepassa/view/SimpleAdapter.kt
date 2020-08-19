/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.view.SimpleAdapter.Holder

/**
 * list适配器
 */
class SimpleAdapter(
  context: Context,
  data: List<SimpleItemEntity>
) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

  override fun getViewHolder(
    convertView: View?,
    viewType: Int
  ): Holder {
    return Holder(convertView!!)
  }

  override fun setLayoutId(type: Int): Int {
    return R.layout.item_path_type
  }

  override fun bindData(
    holder: Holder?,
    position: Int,
    item: SimpleItemEntity?
  ) {
    holder!!.icon.setImageResource(item!!.icon)
    holder.title.text = item.title
    holder.des.text = item.subTitle
  }

  class Holder(view: View) : AbsHolder(view) {
    val icon: AppCompatImageView = view.findViewById(R.id.icon)
    val title: TextView = view.findViewById(R.id.title)
    val des: TextView = view.findViewById(R.id.des)
  }
}