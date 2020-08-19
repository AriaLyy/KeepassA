/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.search

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.search.SearchAdapter.BaseHolder

/**
 * 搜索列表适配器
 */
class SearchAdapter(
  context: Context,
  val data: ArrayList<SimpleItemEntity>,
  val delListener: OnClickListener
) : AbsRVAdapter<SimpleItemEntity, BaseHolder>(context, data) {

  override fun getViewHolder(
    convertView: View,
    viewType: Int
  ): BaseHolder {
    return if (viewType == 1) {
      RecordHolder(convertView)
    } else {
      SearchHolder(convertView)
    }
  }

  override fun setLayoutId(type: Int): Int {
    return if (type == 1) {
      R.layout.item_search_record
    } else {
      R.layout.item_search_result
    }
  }

  override fun bindData(
    holder: BaseHolder,
    position: Int,
    item: SimpleItemEntity
  ) {
    holder.text.text = item.title
    if (holder is RecordHolder) {
      holder.del.tag = position
      holder.del.setOnClickListener(delListener)
    } else if (holder is SearchHolder) {
      holder.des.text = item.subTitle
      IconUtil.setEntryIcon(context, item.obj as PwEntry, holder.icon)
    }
  }

  override fun getItemViewType(position: Int): Int {
    return data[position].id
  }

  open class BaseHolder(view: View) : AbsHolder(view) {
    val icon: AppCompatImageView = view.findViewById(R.id.icon)
    val text: TextView = view.findViewById(R.id.text)

  }

  private class RecordHolder(view: View) : BaseHolder(view) {
    val del: AppCompatImageView = view.findViewById(R.id.del)
  }

  private class SearchHolder(view: View) : BaseHolder(view) {
    val des: TextView = view.findViewById(R.id.des)
  }

}