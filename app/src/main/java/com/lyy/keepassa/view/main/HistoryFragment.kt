/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.utils.Types
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentEntryRecordBinding
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.SimpleEntryAdapter
import com.lyy.keepassa.view.menu.EntryPopMenu
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import java.util.UUID

class HistoryFragment : BaseFragment<FragmentEntryRecordBinding>() {

  private lateinit var module: MainModule
  private lateinit var adapter: SimpleEntryAdapter
  private val entryData = ArrayList<SimpleItemEntity>()
  private var curx = 0

  override fun initData() {
    EventBusHelper.reg(this)
    module = ViewModelProvider(this).get(MainModule::class.java)
    adapter = SimpleEntryAdapter(requireContext(), entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter

    RvItemClickSupport.addTo(binding.list)
        .setOnItemClickListener { _, position, v ->
          val item = entryData[position]
          val icon = v.findViewById<AppCompatImageView>(R.id.icon)
          KeepassAUtil.turnEntryDetail(requireActivity(), item.obj as PwEntry, icon)
        }

    // 长按处理
    RvItemClickSupport.addTo(binding.list)
        .setOnItemLongClickListener { _, position, v ->
          val pop = EntryPopMenu(
              requireActivity(),
              v,
              entryData[position].obj as PwEntry,
              curx
          )
          pop.show()
          true
        }

    // 获取点击位置
    binding.list.addOnItemTouchListener(object : OnItemTouchListener {
      override fun onTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ) {

      }

      override fun onInterceptTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
          curx = e.x.toInt()
        }
        return false
      }

      override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
      }

    })

    initRefresh()

    setData()
  }

  private fun initRefresh() {
    binding.swipe.setColorSchemeColors(
        Color.parseColor("#4E85DB"),
        Color.parseColor("#B48CFF"),
        Color.parseColor("#95DAED")
    )
    binding.swipe.setOnRefreshListener {
      setData()
    }
  }

  private fun setData() {
    module.getEntryHistoryRecord()
        .observe(this, Observer { list ->
          if (list == null) {
            binding.temp.visibility = View.VISIBLE
            binding.swipe.isRefreshing = false
            return@Observer
          }
          entryData.sortByDescending { it.time }
          binding.temp.visibility = View.GONE
          entryData.clear()
          entryData.addAll(list)
          adapter.notifyDataSetChanged()
          binding.swipe.isRefreshing = false
        })
  }

  @Subscribe(threadMode = MAIN)
  fun onAddOrUpdateRecord(record: EntryRecord) {
    if (binding.temp.visibility == View.VISIBLE) {
      binding.temp.visibility = View.GONE
    }
    val newRecordUUid = Types.bytestoUUID(record.uuid)
    // 查找是该记录是否存在
    val oldRecord = entryData.find { (it.obj as PwEntry).uuid == newRecordUUid }

    if (oldRecord == null) {
      val itemData = convertEntry2Item(newRecordUUid, record.time)
      if (itemData != null) {
        entryData.add(itemData)
      }
    } else {
      oldRecord.title = record.title
      oldRecord.subTitle = record.userName
      oldRecord.time = record.time
    }

//    entryData.sortByDescending { it.time }
    adapter.notifyDataSetChanged()
  }

  /**
   * 删除项目
   */
  @Subscribe(threadMode = MAIN)
  fun onDelEvent(delEvent: DelEvent) {
    val pwData = delEvent.pwData
    val item = entryData.find { it.obj == pwData }
    if (item != null) {
      if (pwData is PwEntry) {
        module.delHistoryRecord(pwData)
      }
      entryData.remove(item)
      adapter.notifyDataSetChanged()
    }

  }

  /**
   * 转换记录数据
   */
  private fun convertEntry2Item(
    uuid: UUID,
    time: Long
  ): SimpleItemEntity? {
    val entry = BaseApp.KDB.pm.entries[uuid]
    return if (entry == null) {
      null
    } else {
      val item = SimpleItemEntity()
      item.title = entry.title
      item.subTitle = entry.username
      item.obj = entry
      item.time = time
      item
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_entry_record
  }
}