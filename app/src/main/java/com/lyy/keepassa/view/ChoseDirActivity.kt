/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.google.android.material.snackbar.Snackbar
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.ActivityGroupDirBinding
import com.lyy.keepassa.databinding.FragmentOnlyListBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.event.UndoEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import org.greenrobot.eventbus.EventBus
import java.util.Stack
import java.util.UUID
import kotlin.collections.set

/**
 * 选择目录
 */
class ChoseDirActivity : BaseActivity<ActivityGroupDirBinding>() {

  companion object {
    // 需要恢复的群组id
    const val KEY_GROUP_ID = "KEY_GROUP_ID"

    // 需要恢复的条目id
    const val KEY_ENTRY_ID = "KEY_ENTRY_ID"

    // 1: 恢复群组，2: 恢复项目，3: 选择目录
    const val KEY_TYPE = "KEY_TYPE"

    // 路径地址
    const val DATA_PARENT = "DATA_PARENT"
  }

  private lateinit var curGroup: PwGroup
  private var lastGroupStack: Stack<PwGroup> = Stack()
  private val fragmentMap: HashMap<PwGroupId, DirFragment> = HashMap()
  private lateinit var recycleGroupId: PwGroupId
  private lateinit var recycleEntryId: UUID
  private var loadDialog: LoadingDialog? = null
  private var recycleType = 1
  private var undoGroup: PwGroupV4? = null
  private var undoEntry: PwEntryV4? = null
  private lateinit var module: ChoseDirModule

  override fun setLayoutId(): Int {
    return R.layout.activity_group_dir
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(ChoseDirModule::class.java)
    curGroup = BaseApp.KDB.pm.rootGroup
    recycleType = intent.getIntExtra(KEY_TYPE, 1)
    val gTemp = intent.getSerializableExtra(KEY_GROUP_ID)
    val eTemp = intent.getSerializableExtra(KEY_ENTRY_ID)
    if (recycleType == 1 && gTemp == null) {
      Log.e(TAG, "需要恢复的群组id为空")
      finish()
      return
    }
    if ((recycleType == 2) && eTemp == null) {
      Log.e(TAG, "需要恢复的项目id为空")
      finish()
      return
    }
    if (recycleType != 1 && recycleType != 2 && recycleType != 3) {
      Log.e(TAG, "类型错误")
      finish()
      return
    }

    when (recycleType) {
      1 -> {
        recycleGroupId = gTemp as PwGroupId
      }
      2 -> {
        recycleEntryId = eTemp as UUID
      }
      3 -> {
        binding.bt.text = getString(R.string.choose_dir)
      }
    }

    toolbar.title = curGroup.name

    binding.bt.setOnClickListener {
      loadDialog = LoadingDialog(this)
      when (recycleType) {
        1 -> {
          loadDialog?.show()
          module.undoGroup(recycleGroupId, curGroup)
              .observe(this, Observer { t ->
                undoGroup = t.second
                if (t.first) {
                  onComplete(t.second)
                } else {
                  HitUtil.toaskShort(getString(R.string.save_db_fail))
                }
              })
        }
        2 -> {
          loadDialog!!.show()
          module.undoEntry(recycleEntryId, curGroup)
              .observe(this, Observer { t ->
                undoEntry = t.second
                if (t.first) {
                  onComplete(t.second)
                } else {
                  HitUtil.toaskShort(getString(R.string.save_db_fail))
                }
                loadDialog?.dismiss()
              })
        }
        3 -> {
          intent.putExtra(DATA_PARENT, curGroup.id)
          setResult(Activity.RESULT_OK, intent)
          finishAfterTransition()
        }
      }
    }

    startNextFragment(curGroup, true)
  }

  private fun onComplete(pwGroup: PwGroupV4) {
    loadDialog?.dismiss()
    EventBus.getDefault()
        .post(UndoEvent(1, null, pwGroup))
//    HitUtil.toaskShort(getString(R.string.undo_grouped))
    Snackbar.make(binding.root, getString(R.string.undo_grouped), Snackbar.LENGTH_LONG)
        .setAction("OK") {}
        .show()
    finishAfterTransition()
  }

  private fun onComplete(pwEntryV4: PwEntryV4) {
    loadDialog?.dismiss()
    EventBus.getDefault()
        .post(UndoEvent(2, pwEntryV4, null))
//    HitUtil.toaskShort(getString(R.string.undo_entryed))
    Snackbar.make(binding.root, getString(R.string.undo_entryed), Snackbar.LENGTH_LONG)
        .setAction("OK") {}
        .show()
    finishAfterTransition()
  }

  /**
   * 右 -> 左
   */
  private fun getRlAnim(): Transition {
    return TransitionInflater.from(this)
        .inflateTransition(R.transition.slide_enter)
  }

  /**
   * 左 -> 右
   */
  private fun getLrAnim(): Transition {
    return TransitionInflater.from(this)
        .inflateTransition(R.transition.slide_exit)
  }

  override fun onBackPressed() {
    if (lastGroupStack.empty()) {
      finishAfterTransition()
    } else {
      lastGroupStack.pop()
      super.onBackPressed()
    }
  }

  fun startNextFragment(
    pwGroup: PwGroup,
    isFirst: Boolean = false
  ) {
    if (!isFirst) {
      lastGroupStack.push(curGroup)
    }
    toolbar.title = pwGroup.name
    var fragment = fragmentMap[pwGroup.id]
    if (fragment == null) {
      fragment = DirFragment.generate {
        dirPwGroup = pwGroup
        build()
      }
      fragment.enterTransition = getRlAnim()
      fragment.exitTransition = getLrAnim()
      fragment.reenterTransition = getLrAnim()
      fragmentMap[pwGroup.id] = fragment
    }
    supportFragmentManager.beginTransaction()
        .replace(R.id.content, fragment)
        .addToBackStack("dirStack")
        .commit()
    curGroup = pwGroup
  }

  class DirFragment : BaseFragment<FragmentOnlyListBinding>() {
    private lateinit var adapter: SimpleEntryAdapter
    private val entryData = ArrayList<SimpleItemEntity>()
    lateinit var dirPwGroup: PwGroup

    companion object {
      fun generate(body: DirFragment.() -> DirFragment): DirFragment {
        return with(DirFragment()) { body() }
      }
    }

    fun build(): DirFragment {
      return this
    }

    override fun initData() {

      adapter = SimpleEntryAdapter(requireContext(), entryData)
      binding.list.setHasFixedSize(true)
      binding.list.layoutManager = LinearLayoutManager(context)
      binding.list.adapter = adapter

      entryData.clear()
      for (group in dirPwGroup.childGroups) {
        if (group == BaseApp.KDB.pm.recycleBin) {
          continue
        }
        val item = SimpleItemEntity()
        item.title = group.name
        item.subTitle =
          requireContext().getString(
              R.string.hint_group_desc, KdbUtil.getGroupEntryNum(group)
              .toString()
          )
        item.obj = group
        entryData.add(item)
      }
      adapter.notifyDataSetChanged()

      RvItemClickSupport.addTo(binding.list)
          .setOnItemClickListener { _, position, _ ->
            (activity as ChoseDirActivity).startNextFragment(entryData[position].obj as PwGroup)
          }
    }

    override fun setLayoutId(): Int {
      return R.layout.fragment_only_list
    }

  }
}