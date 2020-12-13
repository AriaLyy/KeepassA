/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.search

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.view.View.OnClickListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.StringUtil
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
  internal var queryString = ""

  companion object {
    val ITEM_TYPE_RECORD = 1
    val ITEM_TYPE_ENTRY = 2
    val ITEM_TYPE_GROUP = 3
  }

  override fun getViewHolder(
    convertView: View,
    viewType: Int
  ): BaseHolder {
    return when (viewType) {
      ITEM_TYPE_RECORD, ITEM_TYPE_GROUP -> RecordHolder(convertView)
      ITEM_TYPE_ENTRY -> SearchHolder(convertView)
      else -> RecordHolder(convertView)
    }
  }

  override fun setLayoutId(type: Int): Int {
    return when (type) {
      ITEM_TYPE_RECORD, ITEM_TYPE_GROUP -> R.layout.item_search_record
      ITEM_TYPE_ENTRY -> R.layout.item_search_result
      else -> R.layout.item_search_record
    }
  }

  override fun bindData(
    holder: BaseHolder,
    position: Int,
    item: SimpleItemEntity
  ) {

    when (item.type) {
      // 历史记录
      ITEM_TYPE_RECORD -> {
        holder.text.text = item.title
        (holder as RecordHolder).del.tag = position
        holder.del.setOnClickListener(delListener)
        holder.del.visibility = View.VISIBLE
        holder.icon.setImageResource(R.drawable.ic_history)
      }
      // 群组
      ITEM_TYPE_GROUP -> {
        highLightText(holder.text, item.title.toString())
        (holder as RecordHolder).del.visibility = View.GONE
        holder.icon.setImageDrawable(
            ResUtil.getSvgIcon(
                R.drawable.ic_folder_24px,
                R.color.colorPrimary
            )
        )
      }

      // 条目
      ITEM_TYPE_ENTRY -> {
        highLightText(holder.text, item.title.toString())
        highLightText((holder as SearchHolder).des, item.subTitle.toString())
        IconUtil.setEntryIcon(context, item.obj as PwEntry, holder.icon)
      }
    }
  }

  private fun highLightText(
    tv: TextView,
    str: String
  ) {
    if (queryString.isNotEmpty() && str.contains(queryString, ignoreCase = true)) {
      val temp: SpannableStringBuilder? = StringUtil.highLightStr(
          str,
          queryString,
          ResUtil.getColor(android.R.color.holo_red_light),
          true
      )

      if (temp == null) {
        tv.text = str
        return
      }
      tv.text = temp
      return
    }
    tv.text = str
  }

  override fun getItemViewType(position: Int): Int {
    return data[position].type
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