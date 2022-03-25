/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.transition.addListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.common.SortType.CHAR_ASC
import com.lyy.keepassa.common.SortType.CHAR_DESC
import com.lyy.keepassa.common.SortType.NONE
import com.lyy.keepassa.common.SortType.TIME_ASC
import com.lyy.keepassa.common.SortType.TIME_DESC
import com.lyy.keepassa.databinding.ActivityGroupDetailBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.entity.showPopMenu
import com.lyy.keepassa.event.CreateOrUpdateEntryEvent
import com.lyy.keepassa.event.CreateOrUpdateGroupEvent
import com.lyy.keepassa.event.DelEvent
import com.lyy.keepassa.event.MoveEvent
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.doOnInterceptTouchEvent
import com.lyy.keepassa.view.SimpleEntryAdapter
import com.lyy.keepassa.widget.MainExpandFloatActionButton
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * 群组详情、回收站详情
 */
@Route(path = "/group/detail")
class GroupDetailActivity : BaseActivity<ActivityGroupDetailBinding>() {

  companion object {
    const val KEY_TITLE = "KEY_TITLE"
    const val KEY_GROUP_ID = "KEY_V3_GROUP_ID"
    const val KEY_IS_IN_RECYCLE_BIN = "KEY_IS_IN_RECYCLE_BIN"
  }

  private lateinit var module: GroupDetailModule
  private lateinit var adapter: SimpleEntryAdapter
  private val entryData = ArrayList<SimpleItemEntity>()
  private var curx = 0

  @JvmField
  @Autowired(name = KEY_IS_IN_RECYCLE_BIN)
  var isRecycleBin = false

  @Autowired(name = KEY_GROUP_ID)
  lateinit var groupId: PwGroupId

  @Autowired(name = KEY_TITLE)
  lateinit var groupTitle: String

  override fun setLayoutId(): Int {
    return R.layout.activity_group_detail
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)


    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)

    // 检查是否是在回收站中
    if (!isRecycleBin) {
      isRecycleBin =
        BaseApp.isV4 && BaseApp.KDB!!.pm.recycleBin != null && BaseApp.KDB!!.pm.recycleBin.id == groupId
    }
    if (isRecycleBin) {
      binding.fab.visibility = View.GONE
    }

    binding.ctlCollapsingLayout.title = groupTitle
    binding.kpaToolbar.title = groupTitle
    binding.kpaToolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (window.enterTransition == null || !KeepassAUtil.instance.isDisplayLoadingAnim()) {
      loadData()
      return
    }
    window.enterTransition?.addListener(onStart = {
      binding.laAnim.speed = 2.5f
      binding.laAnim.playAnimation()
      binding.laAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          super.onAnimationEnd(animation)
          loadData()
        }
      })
    })
  }

  private fun loadData() {
    binding.kpaToolbar.inflateMenu(R.menu.menu_group_detail)
    module = ViewModelProvider(this).get(GroupDetailModule::class.java)
    initList()
    initFab()
    initMenu()
  }

  private fun initMenu() {
    binding.kpaToolbar.setOnMenuItemClickListener {
      val type = when (it.itemId) {
        R.id.sort_down_by_char -> {
          CHAR_DESC
        }
        R.id.sort_up_by_char -> {
          CHAR_ASC
        }
        R.id.sort_down_by_time -> {
          TIME_DESC
        }
        R.id.sort_up_by_time -> {
          TIME_ASC
        }
        else -> NONE
      }
      if (type != NONE) {
        module.sortData(type, entryData)
          .observe(this) { sortData ->
            entryData.clear()
            entryData.addAll(sortData)
            adapter.notifyDataSetChanged()
          }
      }
      return@setOnMenuItemClickListener true
    }
  }

  /**
   * fab
   */
  private fun initFab() {
    if (isRecycleBin) {
      binding.fab.visibility = View.GONE
      return
    }
    binding.fab.setOnItemClickListener(object : MainExpandFloatActionButton.OnItemClickListener {
      override fun onKeyClick() {
        Routerfit.create(ActivityRouter::class.java, this@GroupDetailActivity)
          .toCreateEntryActivity(
            groupId,
            ActivityOptionsCompat.makeSceneTransitionAnimation(this@GroupDetailActivity)
          )
        binding.fab.hintMoreOperate()
      }

      override fun onGroupClick() {
        Routerfit.create(DialogRouter::class.java)
          .showCreateGroupDialog(BaseApp.KDB!!.pm.groups[groupId] ?: BaseApp.KDB!!.pm.rootGroup)
        binding.fab.hintMoreOperate()
      }
    })
  }

  private fun initList() {
    adapter = SimpleEntryAdapter(this, entryData)
    binding.list.setHasFixedSize(true)
    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.adapter = adapter

    getData()

    RvItemClickSupport.addTo(binding.list)
      .setOnItemClickListener { _, position, v ->
        val item = entryData[position]
        if (item.obj is PwGroup) {
          val group = item.obj as PwGroup
          Routerfit.create(ActivityRouter::class.java, this).toGroupDetailActivity(
            groupName = group.name,
            groupId = group.id,
            isRecycleBin = isRecycleBin,
            opt = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
          )
          return@setOnItemClickListener
        }

        if (item.obj is PwEntry) {
          val icon = v.findViewById<AppCompatImageView>(R.id.icon)
          KeepassAUtil.instance.turnEntryDetail(this, item.obj as PwEntry, icon)
          return@setOnItemClickListener
        }
      }

    RvItemClickSupport.addTo(binding.list)
      .setOnItemLongClickListener { _, position, v ->
        entryData[position].showPopMenu(this, v, curx, isRecycleBin)
        true
      }

    // 获取点击位置
    binding.list.doOnInterceptTouchEvent { rv, e ->
      if (e.action == MotionEvent.ACTION_DOWN) {
        curx = e.x.toInt()
      }
      return@doOnInterceptTouchEvent false
    }
  }

  private fun getData() {
    module.getGroupData(this, groupId)
      .observe(this, Observer { list ->
        binding.laAnim.cancelAnimation()
        binding.laAnim.visibility = View.GONE
        binding.fab.visibility = View.VISIBLE
        if (list == null || list.size == 0) {
          // 设置appbar为收缩状态
          binding.appBar.setExpanded(false, false)
          getEmptyLayout().visibility = View.VISIBLE
          binding.list.visibility = View.GONE
          return@Observer
        }
        binding.list.visibility = View.VISIBLE
        getEmptyLayout().visibility = View.GONE
        entryData.clear()
        entryData.addAll(list)
        adapter.notifyDataSetChanged()
      })
  }

  private fun getEmptyLayout(): View {
    if (!binding.vsEmpty.isInflated) {
      binding.vsEmpty.viewStub?.inflate()
    }
    return binding.vsEmpty.root
  }

  /**
   * 创建条目
   */
  @Subscribe(threadMode = MAIN)
  fun onEntryCreate(event: CreateOrUpdateEntryEvent) {
    if (event.entry.parent.id == groupId) {
      if (event.isUpdate) {
        val entry: SimpleItemEntity? = entryData.find { it.obj == event.entry }
        entry?.let {
          val pos = entryData.indexOf(it)
          entryData[pos] = KeepassAUtil.instance.convertPwEntry2Item(event.entry)
          adapter.notifyItemChanged(pos)
        }
        return
      }
      getData()
    }
  }

  /**
   * 创建群组
   */
  @Subscribe(threadMode = MAIN)
  fun onGroupCreate(event: CreateOrUpdateGroupEvent) {
    if (event.pwGroup.parent.id != groupId) {
      return
    }
    getData()
  }

  /**
   * 删除群组
   */
  @Subscribe(threadMode = MAIN)
  fun onDelGroup(delEvent: DelEvent?) {
    if (delEvent == null) {
      return
    }
    getData()
  }

  /**
   * 有条目移动或有条目从回收站中撤回
   */
  @Subscribe(threadMode = MAIN)
  fun onMove(event: MoveEvent) {
    getData()
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }
}