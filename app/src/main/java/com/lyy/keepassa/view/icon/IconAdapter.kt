package com.lyy.keepassa.view.icon

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.bumptech.glide.Glide
import com.keepassdroid.database.PwIconCustom
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.icon.IconAdapter.Holder

/**
 * icon 适配器
 */
class IconAdapter(
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
    return R.layout.item_icon
  }

  override fun bindData(
    holder: Holder?,
    position: Int,
    item: SimpleItemEntity?
  ) {
    if (item!!.icon != -1) {
      Glide.with(context)
          .load(IconUtil.getIconById(item.icon))
          .error(IconUtil.getIconById(0))
          .into(holder!!.img)
    } else {
      Glide.with(context)
          .load((item.obj as PwIconCustom).imageData)
          .error(R.drawable.ic_image_broken_24px)
          .into(holder!!.img)
    }
    val index = item.id + 1
    holder.num.text = index.toString()

  }

  class Holder(view: View) : AbsHolder(view) {
    val img: AppCompatImageView = view.findViewById(R.id.img)
    val num: TextView = view.findViewById(R.id.num)
  }

}