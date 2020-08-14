package com.lyy.keepassa.base

/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager
import com.arialyy.frame.core.AbsActivity
import com.gyf.immersionbar.ImmersionBar
import com.lyy.keepassa.R
import com.lyy.keepassa.util.AutoLockDbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.view.launcher.LauncherActivity

/**
 * Created by Lyy on 2016/9/27.
 */
abstract class BaseActivity<VB : ViewDataBinding> : AbsActivity<VB>() {

  protected var useAnim = true

  //  private val mCompositeDisposable = CompositeDisposable()
  protected lateinit var toolbar: Toolbar

  override fun initData(savedInstanceState: Bundle?) {
    try {
      toolbar = findViewById(R.id.kpa_toolbar)
      toolbar.setNavigationOnClickListener { finishAfterTransition() }
    } catch (e: Exception) {
//      e.printStackTrace()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 进入系统多任务，界面变空白，设置无法截图
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
    if (useAnim) {
      setWindowAnim()
    }

    ImmersionBar.with(this)
        .statusBarColor(R.color.background_color)
        .autoStatusBarDarkModeEnable(true, 0.2f) //自动状态栏字体变色，必须指定状态栏颜色才可以自动变色哦
        .flymeOSStatusBarFontColor(R.color.text_black_color)
        .fitsSystemWindows(true)
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

  private fun setWindowAnim() {
    // salide 为滑入，其它动画效果参考：https://github.com/lgvalle/Material-Animations
    // 第一次进入activity的动画
    window.enterTransition = TransitionInflater.from(this)
        .inflateTransition(R.transition.slide_enter)

    // 退出当前activity的动画
    window.exitTransition = TransitionInflater.from(this)
        .inflateTransition(R.transition.slide_exit)

//    // 重新进入activity的动画
//    window.returnTransition = TransitionInflater.from(this)
//        .inflateTransition(R.transition.slide_return)

  }

  protected fun showQuickUnlockDialog() {
    val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getBoolean(getString(R.string.set_quick_unlock), false)
    if (isOpenQuickLock) {
      BaseApp.APP.startActivity(Intent(Intent.ACTION_MAIN).also {
        it.component =
          ComponentName(
              BaseApp.APP.packageName, "${BaseApp.APP.packageName}.view.main.QuickUnlockActivity"
          )
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      })
      return
    }
    NotificationUtil.startDbLocked(this)
    val launcherIntent = Intent(this, LauncherActivity::class.java)
    BaseApp.KDB.clear(this)
    BaseApp.KDB = null
    startActivity(launcherIntent)
    finish()
  }

  override fun dataCallback(
    result: Int,
    data: Any
  ) {
  }

  override fun onResume() {
    super.onResume()
    // 启动定时器

    if (KeepassAUtil.isStartQuickLockActivity(this)) {
      if (BaseApp.isLocked) {
        AutoLockDbUtil.get().startLockWorkerNow()
        return
      }

      if (KeepassAUtil.isRunningForeground(this)) {
        AutoLockDbUtil.get().resetTimer()
        return
      }
    }

  }

}
