/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.service.input.CandidatesAdapter.Holder
import com.lyy.keepassa.util.IconUtil

/**
 * 候选条目适配器
 */
class CandidatesAdapter(
  context: Context,
  data: List<SimpleItemEntity>
) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

  override fun getViewHolder(
    convertView: View,
    viewType: Int
  ): Holder {
    return Holder(convertView)
  }

  override fun setLayoutId(type: Int): Int {
    return R.layout.item_ime_entry
  }

  override fun bindData(
    holder: Holder,
    position: Int,
    item: SimpleItemEntity
  ) {
    val pwEntryV4 = item.obj as PwEntryV4
    IconUtil.setEntryIcon(context, pwEntryV4, holder.icon)
    holder.text.text = pwEntryV4.title
    holder.itemView.isSelected = item.isSelected
  }

  class Holder(view: View) : AbsHolder(view) {
    val icon: ImageView = view.findViewById(R.id.icon)
    val text: TextView = view.findViewById(R.id.text)
  }
}