/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.bumptech.glide.Glide
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityIconBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.ChooseIconActivity.Adapter.Holder

/**
 * 图标选择
 */
class ChooseIconActivity : BaseActivity<ActivityIconBinding>() {
  companion object {
    const val KEY_ICON_TYPE = "KEY_ICON_TYPE"
    const val ICON_TYPE_STANDARD = 1
    const val ICON_TYPE_CUSTOM = 2
    const val KEY_DATA = "KEY_DATA"
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_icon
  }

  private lateinit var data: ArrayList<SimpleItemEntity>
  private lateinit var adapter: Adapter

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    toolbar.title = getString(R.string.choose_icon)

    data = ArrayList()
    adapter = Adapter(this, data)

    binding.list.adapter = adapter
    binding.list.layoutManager = GridLayoutManager(this, 6)
    binding.list.hasFixedSize()

    getIcon()

    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { recyclerView, position, v ->
          val item = data[position]
          val intent = Intent()
          if (item.icon != -1) {
            intent.putExtra(KEY_ICON_TYPE, ICON_TYPE_STANDARD)
            intent.putExtra(KEY_DATA, PwIconStandard(item.icon))
          } else {
            intent.putExtra(KEY_ICON_TYPE, ICON_TYPE_CUSTOM)
            intent.putExtra(KEY_DATA, item.obj as PwIconCustom)
          }

          setResult(Activity.RESULT_OK, intent)
          finishAfterTransition()
        }
  }

  /**
   * 组装icon数据
   */
  private fun getIcon() {
    for (i in 0..68) {
      val item = SimpleItemEntity()
      item.id = i
      item.icon = i
      data.add(item)
    }
    if (BaseApp.isV4) {
      var i = 69
      val v4Group = BaseApp.KDB.pm as PwDatabaseV4
      for (icon in v4Group.customIcons) {
        val item = SimpleItemEntity()
        item.id = i
        item.icon = -1
        item.obj = icon
        data.add(item)
        i++
      }
    }
    adapter.notifyDataSetChanged()
  }

  /**
   * icon 适配器
   */
  private class Adapter(
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
      holder.num.text = item.id.toString()

    }

    private class Holder(view: View) : AbsHolder(view) {
      val img: AppCompatImageView = view.findViewById(R.id.img)
      val num: TextView = view.findViewById(R.id.num)
    }

  }

}