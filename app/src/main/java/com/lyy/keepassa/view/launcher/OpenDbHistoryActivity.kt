/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityOnlyListBinding
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.event.DbHistoryEvent
import com.lyy.keepassa.util.doOnInterceptTouchEvent
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.doOnItemLongClickListener
import com.lyy.keepassa.view.SimpleAdapter
import com.lyy.keepassa.view.StorageType
import org.greenrobot.eventbus.EventBus

/**
 * 数据库打开记录列表
 */
class OpenDbHistoryActivity : BaseActivity<ActivityOnlyListBinding>() {

  private val data: ArrayList<SimpleItemEntity> = ArrayList()
  private lateinit var adapter: SimpleAdapter
  private lateinit var module: OpenDbHistoryModule
  private var curx = 0

  override fun setLayoutId(): Int {
    return R.layout.activity_only_list
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this)[OpenDbHistoryModule::class.java]
    toolbar.title = getString(R.string.history_record)
    adapter = SimpleAdapter(this, data)
    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.setHasFixedSize(true)
    binding.list.adapter = adapter

    module.getDbOpenRecordList(this)
      .observe(this, Observer { list ->
        if (list != null && list.isNotEmpty()) {
          data.addAll(list)
          adapter.notifyDataSetChanged()
        }
        if (data.size > 0) {
          binding.temp.visibility = View.GONE
        } else {
          binding.temp.visibility = View.VISIBLE
        }
      })

    binding.list.doOnItemClickListener { _, position, _ ->
      val record = data[position].obj as DbHistoryRecord
      finishAfterTransition()
      EventBus.getDefault()
        .post(
          ChangeDbEvent(
            dbName = record.dbName,
            localFileUri = Uri.parse(record.localDbUri),
            cloudPath = record.cloudDiskPath,
            uriType = StorageType.valueOf(record.type),
            keyUri = if (TextUtils.isEmpty(record.keyUri)) null else Uri.parse(
              record.keyUri
            )
          )
        )
    }

    binding.list.doOnItemLongClickListener { _, position, v ->
      showDelPopMenu(position, v)
      return@doOnItemLongClickListener true
    }

    // 获取点击位置
    binding.list.doOnInterceptTouchEvent { _, e ->
      if (e.action == MotionEvent.ACTION_DOWN) {
        curx = e.x.toInt()
      }
      return@doOnInterceptTouchEvent false
    }
  }

  private fun showDelPopMenu(
    position: Int,
    v: View
  ) {
    val popM = DelHistoryPopMenu(this, v, curx)
    popM.getPopMenu()
      .setOnMenuItemClickListener {
        val item = data[position]
        module.deleteHistoryRecord(item)
        data.removeAt(position)
        adapter.notifyItemRemoved(position)
        if (data.isEmpty()) {
          binding.temp.visibility = View.VISIBLE
          EventBus.getDefault().post(DbHistoryEvent(true))
        }
        return@setOnMenuItemClickListener true
      }
    popM.show()
  }

  override fun onDestroy() {
    super.onDestroy()
  }
}