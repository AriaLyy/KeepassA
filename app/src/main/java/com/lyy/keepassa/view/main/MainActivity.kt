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
import android.animation.PropertyValuesHolder
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.util.Pair
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.BarUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.tabs.TabLayoutMediator
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityMainBinding
import com.lyy.keepassa.event.CheckEnvEvent
import com.lyy.keepassa.event.ModifyDbNameEvent
import com.lyy.keepassa.event.ShowTOTPEvent
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.router.FragmentRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.ThemeUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.loadImg
import com.lyy.keepassa.util.transformation.WhiteBgBlurTransformation
import com.lyy.keepassa.view.search.SearchDialog
import com.lyy.keepassa.widgets.MainFloatActionButton
import com.lyy.keepassa.widgets.arc.FloatingActionMenu
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber

@Route(path = "/main/ac")
class MainActivity : BaseActivity<ActivityMainBinding>(), View.OnClickListener {

  private var reenterListener: ReenterListener? = null
  private lateinit var module: MainModule

  companion object {
    private const val MIN_SCALE = 0.85f
    private const val MIN_ALPHA = 0.5f

    // 打开搜索
    const val OPEN_SEARCH = 1
    const val KEY_IS_SHORTCUTS = "isShortcuts"
    const val KEY_SHORTCUTS_TYPE = "shortcutsType"
  }

  @Autowired(name = KEY_IS_SHORTCUTS)
  @JvmField
  var isShortcuts = false

  @Autowired(name = KEY_SHORTCUTS_TYPE)
  @JvmField
  var shortcutType = 1

  private var fabMenu: FloatingActionMenu? = null

  private val historyFm by lazy {
    Routerfit.create(FragmentRouter::class.java).toMainHistoryFragment()
  }

  private val entryFm by lazy {
    Routerfit.create(FragmentRouter::class.java).toMainHomeFragment()
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_main
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    Timber.d("MainActivity")
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this)[MainModule::class.java]

    // 处理快捷方式进入的情况
    if (isShortcuts) {
      // 启动搜索页
      if (shortcutType == OPEN_SEARCH) {
        showSearchDialog()
      }
    }
    module.setEcoIcon(this, binding.dbName)
    lifecycleScope.launch {
      binding.root.post {
        module.startRevealAnim(binding)
      }
    }

    initData()
    initVpAnim()
    initMenu()
    loadThemeIcon()
  }

  private fun loadThemeIcon(){
    // Glide.with(this)
    //   .load(ThemeUtil.getThemeMainBg())
    //   .apply(RequestOptions.bitmapTransform(WhiteBgBlurTransformation(20, 2)))
    //   .into(binding.ivTheme)
    binding.ivTheme.loadImg(ThemeUtil.getThemeMainBg())
  }

  private fun initMenu() {
    val menuKey = KpaUtil.buildMenuIcon(
      this,
      ResUtil.getSvgIcon(R.drawable.ic_password, R.color.color_FFFFFF)
    ) {
      Routerfit.create(ActivityRouter::class.java).toCreateEntryActivity(null)
      fabMenu?.close(true)
    }
    val menuGroup = KpaUtil.buildMenuIcon(this, ResUtil.getDrawable(R.drawable.ic_fab_dir)) {
      Routerfit.create(DialogRouter::class.java)
        .showCreateGroupDialog(BaseApp.KDB!!.pm.rootGroup as PwGroupV4)
      fabMenu?.close(true)
    }

    val menuLock =
      KpaUtil.buildMenuIcon(
        this,
        ResUtil.getSvgIcon(R.drawable.ic_lock_24px, R.color.color_FFFFFF)
      ) {
        showQuickUnlockDialog()
      }

    val coords = IntArray(2)
    fabMenu = FloatingActionMenu.Builder(this@MainActivity)
      .setStateChangeListener(object : FloatingActionMenu.MenuStateChangeListener {
        override fun onMenuOpened(menu: FloatingActionMenu?) {
          binding.fabNew.rotation = 0f
          val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 45f)
          val animation: ObjectAnimator =
            ObjectAnimator.ofPropertyValuesHolder(binding.fabNew, pvhR)
          animation.start()
        }

        override fun onMenuClosed(menu: FloatingActionMenu?) {
          binding.fabNew.rotation = 45f
          val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 0f)
          val animation: ObjectAnimator =
            ObjectAnimator.ofPropertyValuesHolder(binding.fabNew, pvhR)
          animation.start()
        }
      })
      .addSubActionView(menuKey)
      .addSubActionView(menuGroup)
      .addSubActionView(menuLock)
      .setPointInterceptor { mainActionView ->

        if (coords[0] != 0) {
          return@setPointInterceptor Point(coords[0], coords[1])
        }

        mainActionView.getLocationOnScreen(coords)

        coords[1] -= BarUtils.getStatusBarHeight()
        coords[0] += mainActionView.measuredWidth / 2
        coords[1] += mainActionView.measuredHeight / 2

        Timber.d("x: ${coords[0]}, y: ${coords[1]}")

        return@setPointInterceptor Point(coords[0], coords[1])
      }
      .attachTo(binding.fabNew)
      .build()

    binding.fabNew.setImageDrawable(module.getAddIcon())
    binding.fabNew.callback = object : MainFloatActionButton.OnOperateCallback {
      override fun onHint(view: MainFloatActionButton) {
        if (fabMenu?.isOpen == true){
          fabMenu?.close(true)
        }
      }
    }
  }

  private fun initData() {
    module.showInfoDialog(this)
    BaseApp.isLocked = false
    binding.headToolbar.setOnClickListener(this)

    binding.dbName.text = BaseApp.dbFileName
    binding.dbVersion.text = BaseApp.dbName
    val needShowTotp = PreferenceManager.getDefaultSharedPreferences(this)
      .getBoolean(getString(R.string.set_key_main_show_totp_tab), true)
    initVP(needShowTotp)
    binding.ivSearch.doClick {
      showSearchDialog()
    }
  }

  private fun initVP(needShowTotp: Boolean) {

    val totpFm = if (needShowTotp) {
      Routerfit.create(FragmentRouter::class.java).toMainTOTPFragment()
    } else {
      null
    }

    Timber.i("initVP")

    val list = arrayListOf<Fragment>()
    list.add(historyFm)
    list.add(entryFm)
    totpFm?.let {
      list.add(it)
    }
    module.checkHasHistoryRecord()
      .observe(this) { hasHistory ->
        binding.vp.adapter = VpAdapter(list, this)
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
        binding.vp.adapter!!.notifyDataSetChanged()

        binding.tab.getTabAt(0)!!.icon = ResUtil.getDrawable(R.drawable.selector_ic_tab_history)
        binding.tab.getTabAt(0)!!.text = getString(R.string.history)
        binding.tab.getTabAt(1)!!.icon = ResUtil.getDrawable(R.drawable.selector_ic_tab_db)
        binding.tab.getTabAt(1)!!.text = getString(R.string.all)

        val totpTab = binding.tab.getTabAt(2)
        totpTab?.let {
          it.icon = ResUtil.getDrawable(R.drawable.selector_ic_tab_token)
          it.text = ResUtil.getString(R.string.kpa_totp)
        }

      }
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
  fun onShowTOTP(event: ShowTOTPEvent) {
    initVP(event.show)
    Timber.d("update totp")
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
    moveTaskToBack(true)
  }

  override fun onPause() {
    super.onPause()
    reenterListener?.isToChangeDb = false
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  /**
   * 跳转数据库切换
   */
  private fun startArrowAnim() {
    val anim = ObjectAnimator.ofFloat(binding.arrow, "rotation", 0f, 180f)
    anim.duration = MainSettingActivity.arrowAnimDuration
    anim.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
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