/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.app.Activity
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * @author laoyuyu
 * @date 2021/3/22
 */
object BarUtil {
  private val TAG = javaClass.simpleName
  private const val MIUI = 1
  private const val FLYME = 2
  private const val ANDROID_M = 3

  fun showStatusBar(
    activity: Activity,
    show: Boolean
  ) {
    val window = activity.window
    var vis: Int = window.decorView.systemUiVisibility
    if (show) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      window.statusBarColor = ResUtil.getColor(R.color.background_color)
      window.navigationBarColor = ResUtil.getColor(R.color.background_color)
      window.decorView.systemUiVisibility =
        (WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
            or View.SYSTEM_UI_FLAG_VISIBLE
            or setMode()
            )
      return
    }
    window.navigationBarColor = ResUtil.getColor(R.color.background_color)
    vis = vis.or(setMode())
    vis = vis.or(View.SYSTEM_UI_FLAG_FULLSCREEN)
    vis = vis.or(View.INVISIBLE)
    window.decorView.systemUiVisibility = vis
  }

  private fun setMode(): Int {
    var mode = 0
    val isNight = KeepassAUtil.instance.isNightMode()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mode = mode.or(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mode = mode.or(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }
    return mode
  }

  /**
   * 直接读取系统里状态栏高度的值，但是无法判断状态栏是否显示
   */
  fun getStatusBarHeight(context: Context):Int {
    var height = 0
    //获取status_bar_height资源的ID
    val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      //根据资源ID获取响应的尺寸值
      height = context.resources.getDimensionPixelSize(resourceId)
    }
    return height
  }

  /**
   * 状态栏是否隐藏
   * @return true 状态栏没有隐藏
   */
  fun statusBarIsVisible(window: Window):Boolean{
    val rectangle = Rect()
    window.decorView.getWindowVisibleDisplayFrame(rectangle)
    val statusBarHeight: Int = rectangle.top
    return statusBarHeight != 0
  }

  /**
   * @param callback true 状态栏隐藏; false状态栏显示
   */
  private fun statusBarIsVisible(
    context: Context,
    callback: (Boolean) -> Unit
  ) {
    val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager?
    val p = LayoutParams()
    p.type = LayoutParams.TYPE_SYSTEM_OVERLAY
    //放在左上角
    p.gravity = Gravity.START or Gravity.TOP
    // 不可触摸，不可获得焦点
    p.flags = (LayoutParams.FLAG_NOT_TOUCH_MODAL
        or LayoutParams.FLAG_NOT_TOUCHABLE)

    p.width = 1
    p.height = LayoutParams.MATCH_PARENT
    p.format = PixelFormat.TRANSPARENT
    val helperWnd = View(context) //View helperWnd;

    val vto: ViewTreeObserver = helperWnd.viewTreeObserver
    vto.addOnGlobalLayoutListener {
      val windowParams = IntArray(2)
      val screenParams = IntArray(2)
      helperWnd.getLocationInWindow(windowParams)
      helperWnd.getLocationOnScreen(screenParams)
      // 如果状态栏隐藏，返回0，如果状态栏显示则返回高度
      KLog.d(TAG, "getStatusBarHeight = " + (screenParams[1] - windowParams[1]))
      val b = screenParams[1] - windowParams[1] == 0
      callback.invoke(b)
    }
    wm!!.addView(helperWnd, p)
  }

  /**
   * 状态栏亮色模式，设置状态栏黑色文字、图标，
   * 适配4.4以上版本MIUIV、Flyme和6.0以上版本其他Android
   *
   * @return 1:MIUUI 2:Flyme 3:android6.0
   */
  fun setStatusBarLightMode(activity: Activity): Int {
    var result = 0
    when {
      MIUISetStatusBarLightMode(activity, true) -> {
        result = MIUI
      }
      FLYMESetStatusBarLightMode(activity.window, true) -> {
        result = FLYME
      }
      VERSION.SDK_INT >= VERSION_CODES.M -> {
        activity.window.decorView.systemUiVisibility =
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        result = ANDROID_M
      }
    }
    return result
  }

  /**
   * 已知系统类型时，设置状态栏黑色文字、图标。
   * 适配4.4以上版本MIUIV、Flyme和6.0以上版本其他Android
   *
   * @param type 1:MIUUI 2:Flyme 3:android6.0
   */
  fun setStatusBarLightMode(
    activity: Activity,
    type: Int
  ) {
    when (type) {
      MIUI -> {
        MIUISetStatusBarLightMode(activity, true)
      }
      FLYME -> {
        FLYMESetStatusBarLightMode(activity.window, true)
      }
      ANDROID_M -> {
        activity.window.decorView.systemUiVisibility =
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
      }
    }
  }

  /**
   * 状态栏暗色模式，清除MIUI、flyme或6.0以上版本状态栏黑色文字、图标
   */
  fun setStatusBarDarkMode(
    activity: Activity,
    type: Int
  ) {
    when (type) {
      MIUI -> {
        MIUISetStatusBarLightMode(activity, false)
      }
      FLYME -> {
        FLYMESetStatusBarLightMode(activity.window, false)
      }
      ANDROID_M -> {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
      }
    }
  }

  /**
   * 设置状态栏图标为深色和魅族特定的文字风格
   * 可以用来判断是否为Flyme用户
   *
   * @param window 需要设置的窗口
   * @param dark   是否把状态栏文字及图标颜色设置为深色
   * @return boolean 成功执行返回true
   */
  private fun FLYMESetStatusBarLightMode(
    window: Window?,
    dark: Boolean
  ): Boolean {
    var result = false
    if (window != null) {
      try {
        val lp: LayoutParams = window.attributes
        val darkFlag: Field = LayoutParams::class.java
            .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
        val meizuFlags: Field = LayoutParams::class.java
            .getDeclaredField("meizuFlags")
        darkFlag.isAccessible = true
        meizuFlags.isAccessible = true
        val bit: Int = darkFlag.getInt(null)
        var value: Int = meizuFlags.getInt(lp)
        value = if (dark) {
          value or bit
        } else {
          value and bit.inv()
        }
        meizuFlags.setInt(lp, value)
        window.attributes = lp
        result = true
      } catch (e: Exception) {
      }
    }
    return result
  }

  /**
   * 需要MIUIV6以上
   *
   * @param dark 是否把状态栏文字及图标颜色设置为深色
   * @return boolean 成功执行返回true
   */
  private fun MIUISetStatusBarLightMode(
    activity: Activity,
    dark: Boolean
  ): Boolean {
    var result = false
    val window: Window? = activity.window
    if (window != null) {
      val clazz: Class<*> = window::class.java
      try {
        var darkModeFlag = 0
        val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
        val field: Field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
        darkModeFlag = field.getInt(layoutParams)
        val extraFlagField: Method = clazz.getMethod(
            "setExtraFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        if (dark) {
          extraFlagField.invoke(window, darkModeFlag, darkModeFlag) //状态栏透明且黑色字体
        } else {
          extraFlagField.invoke(window, 0, darkModeFlag) //清除黑色字体
        }
        result = true
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
          //开发版 7.7.13 及以后版本采用了系统API，旧方法无效但不会报错，所以两个方式都要加上
          if (dark) {
            activity.window.decorView.systemUiVisibility =
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
          } else {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
          }
        }
      } catch (e: java.lang.Exception) {
        e.printStackTrace()
      }
    }
    return result
  }

}