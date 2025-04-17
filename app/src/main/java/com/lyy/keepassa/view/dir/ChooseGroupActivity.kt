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
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityGroupDirBinding
import com.lyy.keepassa.router.FragmentRouter
import com.lyy.keepassa.util.handleBottomEdge
import com.lyy.keepassa.view.ChoseDirModule
import com.lyy.keepassa.widget.toPx
import timber.log.Timber
import java.util.Stack
import java.util.UUID
import kotlin.collections.set
import kotlin.math.abs

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
    const val KEY_TYPE = "KEY_TYPE"

    // 恢复群组
    const val DATA_MOVE_GROUP = 1

    // 恢复条目
    const val DATA_MOVE_ENTRY = 2

    // 选择群组
    const val DATA_SELECT_GROUP = 3

    // 路径地址
    const val DATA_PARENT = "DATA_PARENT"

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

  private lateinit var curGroup: PwGroupV4
  private var lastGroupStack: Stack<PwGroup> = Stack()
  private val fragmentMap: HashMap<PwGroupId, DirFragment> = HashMap()
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
    handleEdge2Edge()
    module = ViewModelProvider(this)[ChoseDirModule::class.java]
    curGroup = BaseApp.KDB.pm.rootGroup as PwGroupV4
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
      when (recycleType) {
        // 移动群组
        DATA_MOVE_GROUP -> {
          module.moveGroup(this, recycleGroupId!!, curGroup)
        }

        // 移动条目
        DATA_MOVE_ENTRY -> {
          module.moveEntry(this, recycleEntryId!!, curGroup)
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

  private fun handleEdge2Edge(){
    binding.bt.handleBottomEdge { view, i ->
      view.updateLayoutParams<ConstraintLayout.LayoutParams> {
        bottomMargin = i + 16.toPx()
      }
    }
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
    pwGroup: PwGroupV4,
    isFirst: Boolean = false
  ) {
    if (!isFirst) {
      lastGroupStack.push(curGroup)
    }
    toolbar.title = pwGroup.name
    var fragment = fragmentMap[pwGroup.id]
    if (fragment == null) {
      fragment = Routerfit.create(FragmentRouter::class.java).getDirFragment(
        pwGroup,
        recycleType != DATA_SELECT_GROUP,
        recycleGroupId
      )

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