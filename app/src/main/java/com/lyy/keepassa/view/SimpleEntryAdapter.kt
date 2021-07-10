/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.content.Context
import android.graphics.Paint
import android.view.View
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.SimpleEntryAdapter.Holder

/**
 * list适配器
 */
class SimpleEntryAdapter(
  context: Context,
  data: List<SimpleItemEntity>
) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {
  private var showCheckBox = false

  fun showCheckBox(showCheckBox:Boolean){
    this.showCheckBox = showCheckBox
    notifyDataSetChanged()
  }

  override fun getViewHolder(
    convertView: View?,
    viewType: Int
  ): Holder {
    return Holder(convertView!!)
  }

  override fun setLayoutId(type: Int): Int {
    return R.layout.item_entry
  }

  override fun bindData(
    holder: Holder?,
    position: Int,
    item: SimpleItemEntity
  ) {

    if (item.obj is PwGroup) {
      IconUtil.setGroupIcon(context, item.obj as PwGroup, holder!!.icon)
    } else if (item.obj is PwEntry) {
      IconUtil.setEntryIcon(context, item.obj as PwEntry, holder!!.icon)
      val paint = holder.title.paint
      if ((item.obj as PwEntry).expires()){
        paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
        paint.isAntiAlias = true
      }else{
        paint.flags = 0
      }
    }

    holder!!.title.text = item.title
    holder.des.text = item.subTitle

    if (item.subTitle.isBlank()){
      holder.des.visibility = View.GONE
      (holder.title.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.CENTER_VERTICAL)
    }else{
      holder.des.visibility = View.VISIBLE
      (holder.title.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.CENTER_VERTICAL)
    }

    holder.cb.isVisible = showCheckBox
    if (showCheckBox){
      holder.cb.isChecked = item.isCheck
      holder.cb.setOnCheckedChangeListener { _, isChecked ->
        item.isCheck = isChecked
      }
    }
  }

  class Holder(view: View) : AbsHolder(view) {
    val icon: AppCompatImageView = view.findViewById(R.id.icon)
    val title: TextView = view.findViewById(R.id.title)
    val des: TextView = view.findViewById(R.id.des)
    val cb: CheckBox = view.findViewById(R.id.cb)
  }
}