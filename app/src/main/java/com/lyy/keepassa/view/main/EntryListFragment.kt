/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
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
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.SimpleEntryAdapter
import com.lyy.keepassa.view.menu.EntryPopMenu
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

@Route(path = "/main/fragment/entry")
class EntryListFragment : BaseFragment<FragmentEntryRecordBinding>() {

  companion object {
    const val TYPE_HISTORY = "TYPE_HISTORY"
    const val TYPE_TOTP = "TYPE_TOTP"
  }

  private lateinit var module: EntryListModule
  private lateinit var adapter: SimpleEntryAdapter
  private val entryData = ArrayList<SimpleItemEntity>()
  private var curx = 0
  private var scope = MainScope()

  @Autowired(name = "type")
  @JvmField
  var type = TYPE_HISTORY

  override fun onAttach(context: Context) {
    super.onAttach(context)
    module = ViewModelProvider(this).get(EntryListModule::class.java)
  }

  override fun initData() {
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    adapter = SimpleEntryAdapter(requireContext(), entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter

    RvItemClickSupport.addTo(binding.list)
      .setOnItemClickListener { _, position, v ->
        if (position < 0 || position >= entryData.size) {
          return@setOnItemClickListener
        }
        val item = entryData[position]
        val icon = v.findViewById<AppCompatImageView>(R.id.icon)
        KeepassAUtil.instance.turnEntryDetail(requireActivity(), item.obj as PwEntry, icon)
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
      scope.cancel()
      setData()
    }
  }

  private fun checkScope() {
    if (!scope.isActive) {
      scope = MainScope()
    }
  }

  private fun setData() {
    checkScope()
    scope.launch {
      module.getData(type)
        .collectLatest { list ->
          if (list == null) {
            binding.temp.visibility = View.VISIBLE
            binding.swipe.isRefreshing = false
            return@collectLatest
          }
          entryData.sortByDescending { it.time }
          binding.temp.visibility = View.GONE
          entryData.clear()
          entryData.addAll(list)
          adapter.notifyDataSetChanged()
          binding.swipe.isRefreshing = false
        }
    }
  }

  @Subscribe(threadMode = MAIN)
  fun onAddOrUpdateRecord(record: EntryRecord) {
    if (binding.temp.visibility == View.VISIBLE) {
      binding.temp.visibility = View.GONE
    }
    val newRecordUUid = Types.bytestoUUID(record.uuid)
    // 查找是该记录是否存在
    val oldRecord = entryData.find { (it.obj as PwEntry).uuid == newRecordUUid }

    val entry = BaseApp.KDB.pm.entries[newRecordUUid]
    entry?.let { it ->
      if (oldRecord == null) {
        val itemData = KeepassAUtil.instance.convertPwEntry2Item(it)
        itemData.time = record.time
        entryData.add(itemData)
      } else {
        val itemData = KeepassAUtil.instance.convertPwEntry2Item(it)
        oldRecord.title = record.title
        oldRecord.subTitle = itemData.subTitle
        oldRecord.time = record.time
      }

      entryData.sortByDescending { entry ->
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