/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.utils.Types
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentEntryRecordBinding
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.showPopMenu
import com.lyy.keepassa.event.EntryState.CREATE
import com.lyy.keepassa.event.EntryState.DELETE
import com.lyy.keepassa.event.EntryState.MODIFY
import com.lyy.keepassa.event.EntryState.MOVE
import com.lyy.keepassa.event.EntryState.UNKNOWN
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.doOnInterceptTouchEvent
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber

@Route(path = "/main/fragment/entry")
class EntryListFragment : BaseFragment<FragmentEntryRecordBinding>() {

  companion object {
    const val TYPE_HISTORY = "TYPE_HISTORY"
    const val TYPE_TOTP = "TYPE_TOTP"
  }

  private lateinit var module: EntryListModule
  private lateinit var adapter: SimpleEntryAdapter
  private var curx = 0

  @Autowired(name = "type")
  @JvmField
  var type = TYPE_HISTORY

  override fun onAttach(context: Context) {
    super.onAttach(context)
    module = ViewModelProvider(this)[EntryListModule::class.java]
  }

  override fun initData() {
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    adapter = SimpleEntryAdapter(requireContext(), module.entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter


    RvItemClickSupport.addTo(binding.list)
      .setOnItemClickListener { _, position, v ->
        if (position < 0 || position >= module.entryData.size) {
          return@setOnItemClickListener
        }
        val item = module.entryData[position]
        val icon = v.findViewById<AppCompatImageView>(R.id.icon)
        KeepassAUtil.instance.turnEntryDetail(requireActivity(), item.obj as PwEntry, icon)
      }

    // 长按处理
    RvItemClickSupport.addTo(binding.list)
      .setOnItemLongClickListener { _, position, v ->
        module.entryData[position].showPopMenu(requireActivity(), v, curx)
        true
      }

    // 获取点击位置
    binding.list.doOnInterceptTouchEvent { _, e ->
      if (e.action == MotionEvent.ACTION_DOWN) {
        curx = e.x.toInt()
      }
      return@doOnInterceptTouchEvent false
    }
    initRefresh()
    listenerGetData()
    listenerEntryStateChange()
    setData()
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun listenerGetData() {
    lifecycleScope.launch {
      module.getDataFlow.collectLatest { list ->
        if (list == null) {
          binding.temp.visibility = View.VISIBLE
          binding.swipe.isRefreshing = false
          return@collectLatest
        }
        module.entryData.sortByDescending { it.time }
        binding.temp.visibility = View.GONE
        binding.swipe.isRefreshing = false
        adapter.notifyDataSetChanged()
      }
    }
  }

  /**
   * listener the entry status change, there are states: create, delete, modify, move
   */
  private fun listenerEntryStateChange() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.entryStateChangeFlow.collectLatest {
        if (it.pwEntryV4 == null) {
          return@collectLatest
        }
        when (it.state) {
          CREATE -> {
            module.createNewEntry(adapter, it.pwEntryV4)
          }
          MODIFY -> {
            module.updateModifyEntry(adapter, it.pwEntryV4)
          }
          MOVE -> {
            module.moveEntry(adapter, it.pwEntryV4, it.oldParent!!)
          }
          DELETE -> {
            module.deleteEntry(adapter, it.pwEntryV4, it.oldParent!!)
          }
          UNKNOWN -> {
            Timber.d("un known status")
          }
        }
      }
    }
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
    module.getData(type)
  }

  @Subscribe(threadMode = MAIN)
  fun onAddOrUpdateRecord(record: EntryRecord) {
    if (binding.temp.visibility == View.VISIBLE) {
      binding.temp.visibility = View.GONE
    }
    val newRecordUUid = Types.bytestoUUID(record.uuid)
    // 查找是该记录是否存在
    val oldRecord = module.entryData.find { (it.obj as PwEntry).uuid == newRecordUUid }

    val entry = BaseApp.KDB.pm.entries[newRecordUUid]
    entry?.let { it ->
      if (oldRecord == null) {
        val itemData = KeepassAUtil.instance.convertPwEntry2Item(it)
        itemData.time = record.time
        module.entryData.add(itemData)
      } else {
        val itemData = KeepassAUtil.instance.convertPwEntry2Item(it)
        oldRecord.title = record.title
        oldRecord.subTitle = itemData.subTitle
        oldRecord.time = record.time
      }

      module.entryData.sortByDescending { entry ->
        entry.time
      }
      adapter.notifyDataSetChanged()
    }
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

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_entry_record
  }
}