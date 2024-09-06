/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.view.ViewAnimationUtils
import android.view.autofill.AutofillManager
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.blankj.utilcode.util.ScreenUtils
import com.gyf.immersionbar.ImmersionBar
import com.lyy.keepassa.R
import com.lyy.keepassa.R.layout
import com.lyy.keepassa.base.AnimState
import com.lyy.keepassa.base.AnimState.NOT_ANIM
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityLauncherBinding
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.event.DbHistoryEvent
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.FragmentRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.loadImg
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import timber.log.Timber
import kotlin.math.max

@Route(path = "/launcher/activity")
class LauncherActivity : BaseActivity<ActivityLauncherBinding>() {

  private lateinit var module: LauncherModule
  private var changeDbFragment: ChangeDbFragment? = null
  private var openDbFragment: OpenDbFragment? = null
  private var isChangeDb = false
  private var isReady = true
  private var splash: SplashScreen? = null

  /**
   * 启动类型，只有含有历史打开记录时，该记录才有效
   */
  @Autowired(name = KEY_OPEN_TYPE)
  @JvmField
  var type = OPEN_TYPE_OPEN_DB

  private val saveEntityDelegate = SaveEntityDelegate(this)
  private val searchEntityDelegate = SearchEntityDelegate(this)

  override fun setLayoutId(): Int {
    return layout.activity_launcher
  }

  override fun initData(savedInstanceState: Bundle?) {
    splash = installSplashScreen()
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    EventBusHelper.reg(this)
    module = ViewModelProvider(this)[LauncherModule::class.java]
    getAutoFillParam()

    module.showPrivacyAgreement(this)

    initUI()
    module.securityCheck(this)
    splash?.setKeepOnScreenCondition {
      isReady
    }
    // handleSplashScreen()
  }

  private fun handleSplashScreen() {
    splash?.setOnExitAnimationListener { vp ->
      val lottieView = binding.animationView
      lottieView.enableMergePathsForKitKatAndAbove(true)

      // We compute the delay to wait for the end of the splash screen icon
      // animation.
      // val splashScreenAnimationEndTime =
      //   Instant.ofEpochMilli(vp.iconAnimationStartMillis + vp.iconAnimationDurationMillis)
      // val delay = Instant.now(Clock.systemUTC()).until(
      //   splashScreenAnimationEndTime,
      //   ChronoUnit.MILLIS
      // )
      //
      // // Once the delay expires, we start the lottie animation
      lottieView.postDelayed({
        vp.view.alpha = 0f
        vp.iconView.alpha = 0f
        lottieView.playAnimation()
      }, 100)

      // lottieView.playAnimation()

      // Finally we dismiss display our app content using a
      // nice circular reveal

      lottieView.addAnimatorListener(object : AnimatorListenerAdapter() {

        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          val animator = ViewAnimationUtils.createCircularReveal(
            binding.root,
            ScreenUtils.getScreenWidth() / 2,
            ScreenUtils.getScreenHeight() / 2,
            0f,
            max(ScreenUtils.getScreenWidth().toFloat(), ScreenUtils.getScreenHeight().toFloat())
          ).setDuration(600)

          animator.start()
          animator.doOnEnd {
            isReady = false
            binding.animationView.isVisible = false
          }
        }

      })
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    getAutoFillParam()
  }

  private fun getAutoFillParam() {
    module.autoFillParam = intent.getParcelableExtra(KEY_AUTO_FILL_PARAM)
    module.autoFillParam?.let {
      module.autoFillDelegate = if (it.isSave) {
        saveEntityDelegate
      } else {
        searchEntityDelegate
      }
    }
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun useAnim(): AnimState {
    return NOT_ANIM
  }

  override fun handleStatusBar() {
    // super.handleStatusBar()
    ImmersionBar.with(this)
      .transparentStatusBar()
      .transparentNavigationBar()
      .autoDarkModeEnable(true)
      .autoStatusBarDarkModeEnable(true, 0.2f) //自动状态栏字体变色，必须指定状态栏颜色才可以自动变色哦
      .flymeOSStatusBarFontColor(R.color.text_black_color)
      .fitsSystemWindows(false)
      .autoNavigationBarDarkModeEnable(true, 0.2f) // 自动导航栏图标变色，必须指定导航栏颜色才可以自动变色哦
      .statusBarDarkFont(
        true, 0.2f
      )  //原理：如果当前设备支持状态栏字体变色，会设置状态栏字体为黑色，如果当前设备不支持状态栏字体变色，会使当前状态栏加上透明度，否则不执行透明度
      .init()
  }

  /**
   * 初始化界面
   */
  private fun initUI() {
    // https://www.vecteezy.com/vector-art/3452115-wildlife-elk-in-forest-nature-landscape-vector-illustration
    binding.ivBg.loadImg(R.drawable.bg_app_base_bg)
    module.getLastOpenDbHistory(this)
      .observe(this, Observer { t ->
        val fragment: Fragment
        val tag: String
        if (t == null) {
          val p = buildChangDbFragment()
          tag = p.first
          changeDbFragment = p.second
          fragment = changeDbFragment!!
        } else {
          if (type == OPEN_TYPE_CHANGE_DB) {
            val p = buildChangDbFragment()
            tag = p.first
            changeDbFragment = p.second
            fragment = changeDbFragment!!
          } else {
            val p = buildOpenDbFragment(record = t)
            tag = p.first
            openDbFragment = p.second
            fragment = openDbFragment!!
          }
        }

        supportFragmentManager.beginTransaction()
          .replace(R.id.content, fragment, tag)
          .commitNow()
        isReady = false
      })
  }

  override fun onStart() {
    super.onStart()

    // 如果数据库已经打开，直接启用到主页，用于快捷方式添加数据后返回的情况
    if (BaseApp.KDB != null && !BaseApp.isLocked && !module.isFormAutoFill()) {
      Timber.d("数据库已经打开，进入主页")
      Routerfit.create(ActivityRouter::class.java, this).toMainActivity(
        opt = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
      )
      // superFinish()
    }
  }

  fun superFinish() {
    super.finish()
  }

  /**
   * 切换数据库
   */
  fun changeDb() {
    if (changeDbFragment == null) {
      changeDbFragment = ChangeDbFragment()
    }
    supportFragmentManager.beginTransaction()
      .replace(R.id.content, changeDbFragment!!)
      .commitNow()
    isChangeDb = true
  }

  override fun onBackPressed() {
//    super.onBackPressed()
    if (isChangeDb) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.content, openDbFragment!!)
        .commitNow()
      isChangeDb = false
      return
    }
    super.onBackPressed()
  }

  /**
   * da history is empty
   */
  @Subscribe(threadMode = MAIN)
  fun onHistoryEmpty(event: DbHistoryEvent) {
    if (event.isEmpty) {
      isChangeDb = false
    }
  }

  /**
   * 接收切换数据库的回调
   */
  @Subscribe(threadMode = MAIN)
  fun onDbChange(event: ChangeDbEvent) {
    val record = DbHistoryRecord(
      time = System.currentTimeMillis(),
      type = event.uriType.name,
      localDbUri = event.localFileUri.toString(),
      cloudDiskPath = event.cloudPath,
      keyUri = event.keyUri.toString(),
      dbName = event.dbName
    )
    var tag: String = OpenDbFragment.FM_TAG
    if (openDbFragment == null) {
      val p = buildOpenDbFragment(record)
      tag = p.first
      openDbFragment = p.second
    }
    supportFragmentManager.beginTransaction()
      .replace(R.id.content, openDbFragment!!, tag)
      .commitNowAllowingStateLoss()
    openDbFragment!!.updateData(dbRecord = record)
  }

  private fun buildChangDbFragment(): Pair<String, ChangeDbFragment> {
    var fragment = supportFragmentManager.findFragmentByTag(ChangeDbFragment.FM_TAG)
    if (fragment == null || fragment !is ChangeDbFragment) {
      fragment = Routerfit.create(FragmentRouter::class.java).getChangeDbFragment()
    }
    return Pair(ChangeDbFragment.FM_TAG, fragment)
  }

  private fun buildOpenDbFragment(record: DbHistoryRecord): Pair<String, OpenDbFragment> {
    var fragment = supportFragmentManager.findFragmentByTag(OpenDbFragment.FM_TAG)
    if (fragment == null || fragment !is OpenDbFragment) {
      fragment = Routerfit.create(FragmentRouter::class.java).getOpenDbFragment(record)
    }
    return Pair(OpenDbFragment.FM_TAG, fragment)
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  companion object {

    // Unique autofillId for dataset intents.
    const val KEY_AUTO_FILL_PARAM = "KEY_AUTO_FILL_PARAM"
    const val KEY_OPEN_TYPE = "KEY_OPEN_TYPE"
    const val KEY_PKG_NAME = "DATA_PKG_NAME"
    const val OPEN_TYPE_CHANGE_DB = 1
    const val OPEN_TYPE_OPEN_DB = 2
    const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"

    internal fun startLauncherActivity(
      context: Context,
      flags: Int = -1
    ) {
      context.startActivity(Intent(context, LauncherActivity::class.java).apply {
        if (flags != -1) {
          this.flags = flags
        }
      })
    }

    /**
     * 从通知进入登录页
     */
    internal fun createLauncherPending(context: Context): PendingIntent {
      return Intent(context, LauncherActivity::class.java).let { notificationIntent ->
        PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
      }
    }

    /**
     * 数据库未解锁
     * @param apkPackageName 第三方apk包名
     */
    internal fun getAuthDbIntentSender(
      context: Context,
      apkPackageName: String,
      structure: AssistStructure? = null
    ): IntentSender {
      val intent = Intent(context, LauncherActivity::class.java).also {
        it.putExtra(KEY_AUTO_FILL_PARAM, AutoFillParam(apkPkgName = apkPackageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          structure?.let { stru ->
            it.putExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, stru)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          }
        }

      }
      return PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
        .intentSender
    }

    /**
     * 数据库未解锁，保存数据时打开数据库，并保存
     */
    internal fun <T : Activity> authAndSaveDb(
      context: Context,
      apkPackageName: String,
      userName: String,
      pass: String,
      clazz: Class<T>
    ): IntentSender {
      val intent = Intent(context, clazz).also {
        it.putExtra(
          KEY_AUTO_FILL_PARAM, AutoFillParam(
            apkPkgName = apkPackageName,
            isSave = true,
            saveUserName = userName,
            savePass = pass
          )
        )
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      return PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
        .intentSender
    }
  }
}