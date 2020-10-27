/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.service.input.EntryOtherInfoAdapter.Holder

/**
 * @Author laoyuyu
 * @Description 填充其它信息适配器
 * @Date 2020/10/25
 **/
class EntryOtherInfoAdapter(
  context: Context,
  data: List<SimpleItemEntity>
) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

  class Holder(v: View) : AbsHolder(v) {
    val tvHint: TextView = v.findViewById(R.id.tvHint)
    val tvContent: TextView = v.findViewById(R.id.tvContent)
    val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
  }

  override fun getViewHolder(
    convertView: View?,
    viewType: Int
  ): Holder {
    return Holder(convertView!!)
  }

  override fun setLayoutId(type: Int): Int {
    return R.layout.item_entry_other_info
  }

  override fun bindData(
    holder: Holder,
    position: Int,
    item: SimpleItemEntity
  ) {
    holder.tvHint.text = item.title
    holder.tvContent.text = item.content
    if (item.isProtected) {
      holder.ivIcon.visibility = View.VISIBLE
      holder.ivIcon.isSelected = !item.isSelected
      // 显示密码
      if (item.isSelected) {
        holder.tvContent.transformationMethod = PasswordTransformationMethod.getInstance()
      } else {
        holder.tvContent.transformationMethod = HideReturnsTransformationMethod.getInstance()
      }
      holder.ivIcon.setOnClickListener {
        item.isSelected = !item.isSelected
        holder.ivIcon.isSelected = item.isSelected
        notifyItemChanged(position)
      }
      return
    }
    holder.ivIcon.visibility = View.GONE
  }
}