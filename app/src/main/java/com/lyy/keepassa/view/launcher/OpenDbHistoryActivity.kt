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
import com.lyy.keepassa.base.BaseApp
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
import timber.log.Timber
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        if (!list.isNullOrEmpty()) {
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
      val event = ChangeDbEvent(
        dbName = record.dbName,
        localFileUri = record.localDbUri.toUri(),
        cloudPath = record.cloudDiskPath,
        uriType = StorageType.valueOf(record.type),
        keyUri = if (TextUtils.isEmpty(record.keyUri)) null else record.keyUri.toUri()
      )
      lifecycleScope.launch {
        checkQuickRecord(event.localFileUri.toString())
        EventBus.getDefault().post(event)
      }
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

  private suspend fun checkQuickRecord(localUri: String){
    // 检查快速解锁，如果对应的本地文件名有对应的快速解锁记录，删除该记录
    val unlockDao = BaseApp.appDatabase.quickUnlockDao()
    val unLockRecord = unlockDao.findRecord(localUri)
    if (unLockRecord != null){
      Timber.d("记录存在，删除记录：${localUri}")
      unlockDao.deleteRecord(unLockRecord)
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