/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dir

import android.app.Activity
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityGroupDirBinding
import com.lyy.keepassa.event.MoveEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.view.ChoseDirModule
import com.lyy.keepassa.view.dialog.LoadingDialog
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.Stack
import java.util.UUID
import kotlin.collections.set

/**
 * 选择群组
 */
@Route(path = "/group/choose")
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
      ARouter.getInstance()
        .build("/group/choose")
        .withInt(KEY_TYPE, DATA_SELECT_GROUP)
        .withOptionsCompat(ActivityOptionsCompat.makeSceneTransitionAnimation(context))
        .navigation(context, groupDirRequestCode)
    }

    /**
     * 移动条目
     * @param entryId 条目id
     */
    fun moveEntry(
      context: FragmentActivity,
      entryId: UUID
    ) {
      ARouter.getInstance()
        .build("/group/choose")
        .withInt(KEY_TYPE, DATA_MOVE_ENTRY)
        .withSerializable(KEY_ENTRY_ID, entryId)
        .withOptionsCompat(ActivityOptionsCompat.makeSceneTransitionAnimation(context))
        .navigation(context)
    }

    /**
     * 移动群组
     */
    fun moveGroup(
      context: FragmentActivity,
      groupId: PwGroupId
    ) {
      ARouter.getInstance()
        .build("/group/choose")
        .withInt(KEY_TYPE, DATA_MOVE_GROUP)
        .withSerializable(KEY_GROUP_ID, groupId)
        .withOptionsCompat(ActivityOptionsCompat.makeSceneTransitionAnimation(context))
        .navigation(context)
    }
  }

  private lateinit var curGroup: PwGroup
  private var lastGroupStack: Stack<PwGroup> = Stack()
  private val fragmentMap: HashMap<PwGroupId, DirFragment> = HashMap()
  private var loadDialog: LoadingDialog? = null
  private var undoGroup: PwGroupV4? = null
  private var undoEntry: PwEntryV4? = null
  private lateinit var module: ChoseDirModule

  @Autowired(name = KEY_GROUP_ID)
  @JvmField
  var recycleGroupId: PwGroupId? = null

  @Autowired(name = KEY_ENTRY_ID)
  @JvmField
  var recycleEntryId: UUID? = null

  @Autowired(name = KEY_TYPE)
  @JvmField
  var recycleType = DATA_MOVE_GROUP

  override fun setLayoutId(): Int {
    return R.layout.activity_group_dir
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this).get(ChoseDirModule::class.java)
    curGroup = BaseApp.KDB.pm.rootGroup
    if (recycleType == DATA_MOVE_GROUP && recycleGroupId == null) {
      Timber.e("需要恢复的群组id为空")
      finish()
      return
    }

    if ((recycleType == DATA_MOVE_ENTRY) && recycleEntryId == null) {
      Timber.e("需要恢复的项目id为空")
      finish()
      return
    }


    when (recycleType) {
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
          module.moveGroup(recycleGroupId!!, curGroup)
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
          module.moveEntry(recycleEntryId!!, curGroup)
            .observe(this, Observer { t ->
              if (t == null) {
                HitUtil.toaskShort(getString(R.string.save_db_fail))
                return@Observer
              }
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
      fragment = ARouter.getInstance()
        .build("/group/choose/dir")
        .withSerializable(DirFragment.KEY_CUR_GROUP, pwGroup)
        .withBoolean(DirFragment.KEY_IS_MOVE_GROUP, recycleType != DATA_SELECT_GROUP)
        .withSerializable(DirFragment.KEY_IS_RECYCLE_GROUP_ID, recycleGroupId)
        .navigation() as DirFragment

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
}