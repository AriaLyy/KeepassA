/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
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
import com.lyy.keepassa.event.MoveEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import org.greenrobot.eventbus.EventBus
import java.util.Stack
import java.util.UUID
import kotlin.collections.set

/**
 * 选择群组
 */
class ChooseGroupActivity : BaseActivity<ActivityGroupDirBinding>() {

  companion object {
    // 需要恢复的群组id
    private const val KEY_GROUP_ID = "KEY_GROUP_ID"

    // 需要恢复的条目id
    private const val KEY_ENTRY_ID = "KEY_ENTRY_ID"

    // 1: 恢复群组，2: 恢复项目，3: 选择群组，4: 移动
    private const val KEY_TYPE = "KEY_TYPE"

    // 恢复群组
    private const val DATA_MOVE_GROUP = 1

    // 恢复条目
    private const val DATA_MOVE_ENTRY = 2

    // 选择群组
    private const val DATA_SELECT_GROUP = 3

    // 路径地址
    const val DATA_PARENT = "DATA_PARENT"

    /**
     * 选择群组
     */
    fun chooseGroup(
      context: Activity,
      groupDirRequestCode: Int
    ) {
      val intent = Intent(context, ChooseGroupActivity::class.java)
      intent.putExtra(KEY_TYPE, DATA_SELECT_GROUP)
      context.startActivityForResult(
          intent, groupDirRequestCode,
          ActivityOptions.makeSceneTransitionAnimation(context)
              .toBundle()
      )
    }

    /**
     * 移动条目
     * @param entryId 条目id
     */
    fun moveEntry(
      context: Context,
      entryId: UUID
    ) {
      val intent = Intent(context, ChooseGroupActivity::class.java)
      intent.putExtra(KEY_TYPE, DATA_MOVE_ENTRY)
      intent.putExtra(KEY_ENTRY_ID, entryId)
      if (context is Activity) {
        context.startActivity(
            intent,
            ActivityOptions.makeSceneTransitionAnimation(context)
                .toBundle()
        )
        return
      }
      context.startActivity(intent)
    }

    /**
     * 移动群组
     */
    fun moveGroup(
      context: Context,
      groupId: PwGroupId
    ) {
      val intent = Intent(context, ChooseGroupActivity::class.java)
      intent.putExtra(KEY_TYPE, DATA_MOVE_GROUP)
      intent.putExtra(KEY_GROUP_ID, groupId)
      if (context is Activity) {
        context.startActivity(
            intent,
            ActivityOptions.makeSceneTransitionAnimation(context)
                .toBundle()
        )
        return
      }
      context.startActivity(intent)
    }
  }

  private lateinit var curGroup: PwGroup
  private var lastGroupStack: Stack<PwGroup> = Stack()
  private val fragmentMap: HashMap<PwGroupId, DirFragment> = HashMap()
  private lateinit var recycleGroupId: PwGroupId
  private lateinit var recycleEntryId: UUID
  private var loadDialog: LoadingDialog? = null
  private var recycleType = DATA_MOVE_GROUP
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
    recycleType = intent.getIntExtra(KEY_TYPE, DATA_MOVE_GROUP)
    val gTemp = intent.getSerializableExtra(KEY_GROUP_ID)
    val eTemp = intent.getSerializableExtra(KEY_ENTRY_ID)
    if (recycleType == DATA_MOVE_GROUP && gTemp == null) {
      Log.e(TAG, "需要恢复的群组id为空")
      finish()
      return
    }
    if ((recycleType == DATA_MOVE_ENTRY) && eTemp == null) {
      Log.e(TAG, "需要恢复的项目id为空")
      finish()
      return
    }
    if (recycleType != DATA_MOVE_GROUP
        && recycleType != DATA_MOVE_ENTRY
        && recycleType != DATA_SELECT_GROUP
    ) {
      Log.e(TAG, "类型错误")
      finish()
      return
    }

    when (recycleType) {
      DATA_MOVE_GROUP -> {
        recycleGroupId = gTemp as PwGroupId
      }
      DATA_MOVE_ENTRY -> {
        recycleEntryId = eTemp as UUID
      }
      DATA_SELECT_GROUP -> {
        binding.bt.text = getString(R.string.choose_dir)
      }
    }

    toolbar.title = curGroup.name

    binding.bt.setOnClickListener {
      loadDialog = LoadingDialog(this)
      when (recycleType) {
        // 移动群组
        DATA_MOVE_GROUP -> {
          loadDialog?.show()
          module.moveGroup(recycleGroupId, curGroup)
              .observe(this, Observer { t ->
                undoGroup = t.second
                if (t.first) {
                  onComplete(t.second)
                  return@Observer
                }
                HitUtil.toaskShort(getString(R.string.save_db_fail))
                loadDialog?.dismiss()
              })
        }

        // 移动条目
        DATA_MOVE_ENTRY -> {
          loadDialog!!.show()
          module.moveEntry(recycleEntryId, curGroup)
              .observe(this, Observer { t ->
                undoEntry = t.second
                if (t.first) {
                  onComplete(t.second)
                  return@Observer
                }

                HitUtil.toaskShort(getString(R.string.save_db_fail))
                loadDialog?.dismiss()
              })
        }

        // 选择群组目录
        DATA_SELECT_GROUP -> {
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
        .post(MoveEvent(MoveEvent.MOVE_TYPE_GROUP, null, pwGroup))
    HitUtil.toaskShort(getString(R.string.undo_grouped))
//    Snackbar.make(binding.root, getString(R.string.undo_grouped), Snackbar.LENGTH_LONG)
//        .setAction("OK") {}
//        .show()
    finishAfterTransition()
  }

  private fun onComplete(pwEntryV4: PwEntryV4) {
    loadDialog?.dismiss()
    EventBus.getDefault()
        .post(MoveEvent(MoveEvent.MOVE_TYPE_ENTRY, pwEntryV4, null))
    HitUtil.toaskShort(getString(R.string.undo_entryed))
//    Snackbar.make(binding.root, getString(R.string.undo_entryed), Snackbar.LENGTH_LONG)
//        .setAction("OK") {}
//        .show()
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
            (activity as ChooseGroupActivity).startNextFragment(entryData[position].obj as PwGroup)
          }
    }

    override fun setLayoutId(): Int {
      return R.layout.fragment_only_list
    }

  }
}