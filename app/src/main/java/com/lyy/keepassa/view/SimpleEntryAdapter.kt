package com.lyy.keepassa.view

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
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
    item: SimpleItemEntity?
  ) {

    if (item!!.obj is PwGroup) {
      IconUtil.setGroupIcon(context, item.obj as PwGroup, holder!!.icon)
    } else if (item.obj is PwEntry) {
      IconUtil.setEntryIcon(context, item.obj as PwEntry, holder!!.icon)
    }

    holder!!.title.text = item.title
    holder.des.text = item.subTitle
  }

  /**
   * 处理group的图标
   */

  class Holder(view: View) : AbsHolder(view) {
    val icon: AppCompatImageView = view.findViewById(R.id.icon)
    val title: TextView = view.findViewById(R.id.title)
    val des: TextView = view.findViewById(R.id.des)
  }
}
