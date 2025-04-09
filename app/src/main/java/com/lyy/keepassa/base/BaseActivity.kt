/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Pair
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.databinding.ViewDataBinding
import com.arialyy.frame.core.AbsActivity
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ReflectionUtil
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ScreenUtils
import com.gyf.immersionbar.ImmersionBar
import com.lyy.keepassa.R
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil.isNull
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.view.launcher.LauncherActivity
import me.jessyan.autosize.AutoSizeConfig
import timber.log.Timber
import java.lang.reflect.Field

/**
 * Created by Lyy on 2016/9/27.
 */
abstract class BaseActivity<VB : ViewDataBinding> : AbsActivity<VB>() {

  protected lateinit var toolbar: Toolbar

  companion object {
    var showStatusBar = false
  }

  override fun initData(savedInstanceState: Bundle?) {
    try {
      toolbar = findViewById(R.id.kpa_toolbar)
      toolbar.setNavigationOnClickListener { finishAfterTransition() }
    } catch (e: Exception) {
      // Timber.w(e)
    }
  }

  override fun onPreInit(): Boolean {
    if (!KpaUtil.isHomeActivity(this)
      && (BaseApp.KDB.isNull() || BaseApp.dbRecord == null)
    ) {
      BaseApp.isLocked = true
      HitUtil.toaskShort(getString(R.string.notify_db_locked))
      // Cannot be used finishAfterTransition(), because binding invalid
      showQuickUnlockDialog()

      return false
    }
    return true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    AutoSizeConfig.getInstance().screenHeight = ScreenUtils.getScreenHeight()
    AutoSizeConfig.getInstance().screenWidth = ScreenUtils.getScreenWidth()
    super.onCreate(savedInstanceState)

    // 进入系统多任务，界面变空白，设置无法截图
    if (!AppUtils.isAppDebug()) {
      window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
      )
    }

    handleStatusBar()
  }

  open fun handleStatusBar() {
    ImmersionBar.with(this)
      .statusBarColor(R.color.background_color)
      .autoDarkModeEnable(true)
      .autoStatusBarDarkModeEnable(true, 0.2f) //自动状态栏字体变色，必须指定状态栏颜色才可以自动变色哦
      .flymeOSStatusBarFontColor(R.color.text_black_color)
      .fitsSystemWindows(true)
      // .hideBar(BarHide.FLAG_HIDE_STATUS_BAR)
      .autoNavigationBarDarkModeEnable(true, 0.2f) // 自动导航栏图标变色，必须指定导航栏颜色才可以自动变色哦
      .navigationBarColor(R.color.background_color)
      .statusBarDarkFont(
        true, 0.2f
      )  //原理：如果当前设备支持状态栏字体变色，会设置状态栏字体为黑色，如果当前设备不支持状态栏字体变色，会使当前状态栏加上透明度，否则不执行透明度
      .init()
  }

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(LanguageUtil.setLanguage(newBase!!, BaseApp.currentLang))
  }

  protected fun showQuickUnlockDialog() {
    KeepassAUtil.instance.lock()
    finish()
  }

  override fun onRestart() {
    super.onRestart()
    Timber.d("onRestart")
    if (!KpaUtil.isHomeActivity(this) && (BaseApp.KDB.isNull() || BaseApp.isLocked)) {
      BaseApp.handler.postDelayed({
        KeepassAUtil.instance.lock()
        KeepassAUtil.instance.turnLauncher(LauncherActivity.OPEN_TYPE_OPEN_DB)
      }, 150)
      return
    }
  }

  var isStartOtherActivity = false
  override fun startActivity(
    intent: Intent?,
    options: Bundle?
  ) {
    super.startActivity(intent, options)
    isStartOtherActivity = true
  }

  /**
   * Android10 Activity的onStop方法可能会导致共享元素动画失效，通过反射注入恢复共享元素动画
   * @param activity
   */
  fun updateResume(activity: Activity) {
    if (!isStartOtherActivity) {
      return
    }
    Looper.myQueue()
      .addIdleHandler {
        try {
          Timber.d("updateResume")
          ActivityOptions.makeSceneTransitionAnimation(this)
          val stateField: Field = ReflectionUtil.getField(
            Activity::class.java,
            "mActivityTransitionState"
          )

          val stateObj = stateField.get(activity)
          val activityTransitionStateClazz =
            classLoader.loadClass("android.app.ActivityTransitionState")

          val mPendingExitNamesField: Field = ReflectionUtil.getField(
            activityTransitionStateClazz,
            "mPendingExitNames"
          )
          val b = buildSharedElements()
          mPendingExitNamesField.set(stateObj, b)
        } catch (e: java.lang.Exception) {
          Timber.e(e)
        }
        return@addIdleHandler false
      }
  }

  /**
   * @param sharedElements 共享元素属性
   */
  open fun buildSharedElements(vararg sharedElements: Pair<View, String>): ArrayList<String> {
    val names = ArrayList<String>()
    for (i in sharedElements.indices) {
      val sharedElement: Pair<View, String> = sharedElements[i]
      val sharedElementName = sharedElement.second
        ?: throw IllegalArgumentException("Shared element name must not be null")
      names.add(sharedElementName)
    }
    return names
  }

  override fun onResume() {
    super.onResume()
    // 启动定时器
    KeepassAUtil.instance.startLockTimer(this)
  }
}