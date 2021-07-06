/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.util.Pair
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arialyy.frame.core.AbsFrame
import com.google.android.material.tabs.TabLayoutMediator
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityMainBinding
import com.lyy.keepassa.event.CheckEnvEvent
import com.lyy.keepassa.event.ModifyDbNameEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.create.CreateDbActivity
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.create.CreateGroupDialog
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.search.SearchDialog
import com.lyy.keepassa.widget.MainExpandFloatActionButton
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber

class MainActivity : BaseActivity<ActivityMainBinding>(), View.OnClickListener {

  private var reenterListener: ReenterListener? = null
  private lateinit var module: MainModule
  private lateinit var historyFm: HistoryFragment
  private lateinit var entryFm: EntryFragment

  companion object {
    private const val MIN_SCALE = 0.85f
    private const val MIN_ALPHA = 0.5f

    // 快捷方式类型
    const val SHORTCUTS_TYPE = "shortcutsType"

    // 打开搜索
    const val OPEN_SEARCH = 1

    fun startMainActivity(activity: Activity) {
      val intent = Intent(activity, MainActivity::class.java)
      activity.startActivity(
        intent, ActivityOptions.makeSceneTransitionAnimation(activity)
          .toBundle()
      )
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_main
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this)[MainModule::class.java]

    val isShortcuts = intent.getBooleanExtra("isShortcuts", false)
    Timber.i("isShortcuts = $isShortcuts")

    // 处理快捷方式进入的情况
    if (isShortcuts) {
      val openType = intent.getIntExtra(SHORTCUTS_TYPE, -1)
      // 启动搜索页
      if (openType == OPEN_SEARCH) {
        showSearchDialog()
      }
      if (BaseApp.isLocked) {
        KeepassAUtil.instance.reOpenDb(this)
        return
      }
    }
    module.setEcoIcon(this, binding.dbName)
    initData()
    initVpAnim()
  }

  private fun initData() {
    module.showInfoDialog(this)
    BaseApp.isLocked = false
    binding.headToolbar.setOnClickListener(this)
    binding.search.setOnClickListener(this)
    binding.lock.setOnClickListener(this)

    historyFm = HistoryFragment()
    entryFm = EntryFragment()
    binding.dbName.text = BaseApp.dbFileName
    binding.dbVersion.text = BaseApp.dbName

    module.checkHasHistoryRecord()
      .observe(this, { hasHistory ->
        binding.vp.adapter = VpAdapter(listOf(historyFm, entryFm), this)
        TabLayoutMediator(binding.tab, binding.vp) { tab, position ->
//      tab.text = "OBJECT ${(position + 1)}"
        }.attach()

        // 是否优先显示条目
        val showEntries = PreferenceManager.getDefaultSharedPreferences(this)
          .getBoolean(getString(R.string.set_key_main_allow_show_entries), false)
        if (showEntries) {
          binding.vp.currentItem = 1
        } else {
          binding.vp.currentItem = if (hasHistory) 0 else 1
        }

        binding.tab.getTabAt(0)!!.icon = getDrawable(R.drawable.selector_ic_tab_history)
        binding.tab.getTabAt(0)!!.text = getString(R.string.history)
        binding.tab.getTabAt(1)!!.icon = getDrawable(R.drawable.selector_ic_tab_db)
        binding.tab.getTabAt(1)!!.text = getString(R.string.all)
      })

    binding.fab.setOnItemClickListener(object : MainExpandFloatActionButton.OnItemClickListener {
      override fun onKeyClick() {
        startActivity(
          Intent(this@MainActivity, CreateEntryActivity::class.java),
          ActivityOptions.makeSceneTransitionAnimation(this@MainActivity)
            .toBundle()
        )
        binding.fab.hintMoreOperate()
      }

      override fun onGroupClick() {
        val dialog = CreateGroupDialog.generate {
          parentGroup = BaseApp.KDB!!.pm.rootGroup
          build()
        }
        dialog.show(supportFragmentManager, "CreateGroupDialog")
        binding.fab.hintMoreOperate()
      }

    })
  }

  private fun initVpAnim() {
    binding.vp.setPageTransformer { view, position ->
      view.apply {
        when {
          position < -1 -> { // [-Infinity,-1)
            // This page is way off-screen to the left.
            alpha = 0f
          }
          position <= 1 -> { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
            // Fade the page relative to its size.
            alpha = (MIN_ALPHA +
                (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
          }
          else -> { // (1,+Infinity]
            // This page is way off-screen to the right.
            alpha = 0f
          }
        }
//        binding.tab.getTabAt(0)?.view?.alpha = alpha
//        binding.tab.getTabAt(1)?.view?.alpha = alpha
      }
    }
  }

  @Subscribe(threadMode = MAIN)
  fun onCheckEnv(event: CheckEnvEvent) {
    module.setEcoIcon(this, binding.dbName)
  }

  @Subscribe(threadMode = MAIN)
  fun onDnNameModify(event: ModifyDbNameEvent) {
    binding.dbVersion.text = BaseApp.dbName
  }

  override fun onClick(v: View?) {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    when (v!!.id) {
      R.id.head_toolbar -> startArrowAnim()
      R.id.search -> {
        showSearchDialog()
      }
      R.id.lock -> {
        showQuickUnlockDialog()
      }
    }
  }

  /**
   * 显示搜索对话框
   */
  private fun showSearchDialog() {
    val searchDialog = SearchDialog()
    searchDialog.show(supportFragmentManager, "search_dialog")
  }

  override fun onBackPressed() {
    // 返回键不退出而是进入后台
    moveTaskToBack(false)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    // 需要关闭 LauncherActivity\ InputPassActivity \ CreateActivity 三个界面
    for (ac in AbsFrame.getInstance().activityStack) {
      if (ac is LauncherActivity || ac is CreateDbActivity) {
        ac.rootView.visibility = View.GONE
        ac.finish()
        ac.overridePendingTransition(0, 0)
      }
    }

  }

  override fun onPause() {
    super.onPause()
    reenterListener?.isToChangeDb = false
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  override fun useAnim(): Boolean {
    return false
  }

  /**
   * 跳转数据库切换
   */
  private fun startArrowAnim() {
    val anim = ObjectAnimator.ofFloat(binding.arrow, "rotation", 0f, 180f)
    anim.duration = MainSettingActivity.arrowAnimDuration
    anim.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        // 为了达到更好的效果，先将动画设置为空
        window.enterTransition = null
        window.exitTransition = null
        window.returnTransition = null
        window.reenterTransition = null
//        window.sharedElementReenterTransition.excludeTarget(android.R.id.statusBarBackground, true)
//        window.sharedElementReenterTransition.excludeTarget(
//            android.R.id.navigationBarBackground, true
//        )

        val intent = Intent(this@MainActivity, MainSettingActivity::class.java)
        val appIcon =
          Pair<View, String>(binding.appIcon, getString(R.string.transition_app_icon))
        val dbName =
          Pair<View, String>(binding.dbName, getString(R.string.transition_db_name))
        val dbVersion =
          Pair<View, String>(binding.dbVersion, getString(R.string.transition_db_version))
        val dbLittle =
          Pair<View, String>(binding.arrow, getString(R.string.transition_db_little))
        val option =
          ActivityOptions.makeSceneTransitionAnimation(
            this@MainActivity, appIcon, dbName, dbLittle, dbVersion
          )

        startActivity(intent, option.toBundle())
        if (reenterListener == null) {
          reenterListener = ReenterListener(binding.arrow)
          window.sharedElementReenterTransition.addListener(reenterListener)
        }
        reenterListener?.isToChangeDb = true
      }
    })
    anim.start()
  }

  private class ReenterListener(private val arrow: AppCompatImageView) : TransitionListener {

    var isToChangeDb: Boolean = false
    override fun onTransitionEnd(transition: Transition?) {
      Timber.d("onTransitionEnd")
      if (!isToChangeDb) {
        arrow.rotation = 0f
      }
      isToChangeDb = !isToChangeDb
    }

    override fun onTransitionResume(transition: Transition?) {
      Timber.d("onTransitionResume")
    }

    override fun onTransitionPause(transition: Transition?) {
      Timber.d("onTransitionPause")
    }

    override fun onTransitionCancel(transition: Transition?) {
      Timber.d("onTransitionCancel")
    }

    override fun onTransitionStart(transition: Transition?) {
      Timber.d("onTransitionStart")
    }

  }

  private class VpAdapter(
    private val fragments: List<Fragment>,
    fm: FragmentActivity
  ) : FragmentStateAdapter(fm) {

    override fun getItemCount(): Int {
      return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
      return fragments[position]
    }
  }
}