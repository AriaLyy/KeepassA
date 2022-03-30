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
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.DpUtils
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentOnlyListBinding
import com.lyy.keepassa.entity.EntryType
import com.lyy.keepassa.entity.showPopMenu
import com.lyy.keepassa.event.CreateOrUpdateEntryEvent
import com.lyy.keepassa.event.CreateOrUpdateGroupEvent
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.event.EntryState.CREATE
import com.lyy.keepassa.event.MoveEvent
import com.lyy.keepassa.event.MultiChoiceEvent
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.doOnInterceptTouchEvent
import com.lyy.keepassa.util.doOnItemClickListener
import com.lyy.keepassa.util.doOnItemLongClickListener
import com.lyy.keepassa.util.isAFS
import com.lyy.keepassa.view.SimpleEntryAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber

@Route(path = "/main/fragment/home")
class HomeFragment : BaseFragment<FragmentOnlyListBinding>() {

  private lateinit var module: HomeModule
  private lateinit var adapter: SimpleEntryAdapter
  private var curx = 0
  private var isSyncDb = false

  override fun onAttach(context: Context) {
    super.onAttach(context)
    module = ViewModelProvider(this).get(HomeModule::class.java)
  }

  override fun initData() {
    EventBusHelper.reg(this)
    adapter = SimpleEntryAdapter(requireContext(), module.itemDataList)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = adapter
    binding.list.doOnItemClickListener { _, position, v ->
      val item = module.itemDataList[position]
      if (item.obj == EntryType.TYPE_COLLECTION) {
        Routerfit.create(ActivityRouter::class.java, requireActivity()).toMyCollection(
          ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        )
        return@doOnItemClickListener
      }
      if (item.obj is PwGroup) {
        KeepassAUtil.instance.turnEntryDetail(requireActivity(), item.obj as PwGroup)
      } else if (item.obj is PwEntry) {
        val icon = v.findViewById<AppCompatImageView>(R.id.icon)
        KeepassAUtil.instance.turnEntryDetail(requireActivity(), item.obj as PwEntry, icon)
      }
    }

    val div = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    val drawable = ColorDrawable(Color.TRANSPARENT)
    drawable.bounds = Rect(0, 0, 200, DpUtils.dp2px(20))
    div.setDrawable(drawable)
    binding.list.addItemDecoration(div)

    // 长按处理
    binding.list.doOnItemLongClickListener { _, position, v ->
      module.itemDataList[position].showPopMenu(requireActivity(), v, curx)
      true
    }

    // 获取点击位置
    binding.list.doOnInterceptTouchEvent { _, e ->
      if (e.action == MotionEvent.ACTION_DOWN) {
        curx = e.x.toInt()
      }
      return@doOnInterceptTouchEvent false
    }
    listenerDataGet()
    initRefresh()
    module.getRootEntry()
    listenerCollection()
    listenerEntryStateChange()
  }

  /**
   * listener the entry status change, there are three states: create, delete, and modify.
   */
  private fun listenerEntryStateChange() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.entryStateChangeFlow.collectLatest {
        it.pwEntryV4?.let { entry ->
          when (it.state) {
            CREATE -> {
              module.createNewEntry(adapter, entry)
            }

          }
        }

      }
    }
  }

  /**
   * listener update root data
   */
  private fun listenerDataGet() {
    lifecycleScope.launch {
      module.itemDataFlow.collectLatest {
        adapter.notifyDataSetChanged()
        if (isSyncDb) {
          finishRefresh(true)
        }
      }
    }
  }

  /**
   * listener collection state
   */
  private fun listenerCollection() {
    lifecycleScope.launch {
      KpaUtil.kdbHandlerService.collectionStateFlow.collectLatest {
        if (module.itemDataList.isEmpty()) {
          Timber.d("current no any data, please wait get root data.")
          return@collectLatest
        }

        if (module.itemDataList[0] == module.collectionEntry) {
          module.itemDataList[0].subTitle =
            ResUtil.getString(R.string.current_collection_num, it.collectionNum.toString())
          adapter.notifyItemChanged(0)
          return@collectLatest
        }
        module.collectionEntry.subTitle =
          ResUtil.getString(R.string.current_collection_num, it.collectionNum.toString())
        module.itemDataList.add(0, module.collectionEntry)
        adapter.notifyItemInserted(0)
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
      if (BaseApp.dbRecord == null) {
        finishRefresh(false)
        return@setOnRefreshListener
      }

      if (BaseApp.dbRecord!!.isAFS()) {
        isSyncDb = true
        module.getRootEntry()
        return@setOnRefreshListener
      }

      module.syncDb {
        if (it != DbSynUtil.STATE_SUCCEED) {
          finishRefresh(false)
          return@syncDb
        }
        isSyncDb = true
        module.getRootEntry()
        finishRefresh(true)
      }
    }
  }

  private fun finishRefresh(isSuccess: Boolean) {
    binding.swipe.isRefreshing = false
    HitUtil.snackShort(
      mRootView,
      "${getString(R.string.sync_db)} ${
        if (isSuccess) getString(R.string.success) else getString(
          R.string.fail
        )
      }"
    )
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_only_list
  }

  /**
   * 创建或更新条目
   */
  @Subscribe(threadMode = MAIN)
  fun onEntryCreate(event: CreateOrUpdateEntryEvent) {
//    if (event.isUpdate) {
//      val entry: SimpleItemEntity? = entryData.find { it.obj == event.entry }
//      entry?.let {
//        val pos = entryData.indexOf(it)
//        entryData[pos] = KeepassAUtil.instance.convertPwEntry2Item(event.entry)
//        adapter.notifyItemChanged(pos)
//      }
//      return
//    }
//    module.getRootEntry()
  }

  /**
   * 创建群组
   */
  @Subscribe(threadMode = MAIN)
  fun onGroupCreate(event: CreateOrUpdateGroupEvent) {
//    if (event.pwGroup.parent != BaseApp.KDB.pm.rootGroup) {
//      return
//    }
//    if (event.isUpdate) {
//      val entry: SimpleItemEntity? = entryData.find { it.obj == event.pwGroup }
//      entry?.let {
//
//        val pos = entryData.indexOf(it)
//        entryData[pos] = KeepassAUtil.instance.convertPwGroup2Item(event.pwGroup)
//        adapter.notifyItemChanged(pos)
//      }
//      return
//    }
//    module.getRootEntry()
  }

  /**
   * 回收站中恢复数据
   */
  @Subscribe(threadMode = MAIN)
  fun onMove(event: MoveEvent) {
    module.getRootEntry()
  }

  /**
   * 删除项目
   */
  @Subscribe(threadMode = MAIN)
  fun onDelEvent(delEvent: DelEvent?) {
//    if (delEvent == null) {
//      return
//    }
//    val entry: SimpleItemEntity? = entryData.find { it.obj == delEvent.pwData }
//    if (entry != null) {
//      entryData.remove(entry)
//    }
//    if (BaseApp.isV4 && BaseApp.KDB.pm.recycleBin != null) {
//      val recycleBin = entryData.find { it.obj == BaseApp.KDB.pm.recycleBin }
//      if (recycleBin != null) {
//        recycleBin.subTitle = getString(
//          R.string.hint_group_desc,
//          KdbUtil.getGroupEntryNum(recycleBin.obj as PwGroup)
//            .toString()
//        )
//      }
//    } else {
//      // 如果回收站不存在，重新刷新界面，进行删除操作时会自动添加回收站
//      module.getRootEntry()
//    }
//    adapter.notifyDataSetChanged()
  }

  /**
   * 多选
   */
  @Subscribe(threadMode = MAIN)
  fun onMultiChoice(mcEvent: MultiChoiceEvent) {
    adapter.showCheckBox(true)
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }
}