/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.app.Activity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object PlayUtil {

  fun playServiceExist(context: Activity): Boolean {
    // 验证是否已在此设备上安装并启用Google Play服务，以及此设备上安装的旧版本是否为此客户端所需的版本
    val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (code == ConnectionResult.SUCCESS) {
      // 支持Google服务
      return true
    } else {
      /**
       * 依靠 Play 服务 SDK 运行的应用在访问 Google Play 服务功能之前，应始终检查设备是否拥有兼容的 Google Play 服务 APK。
       * 我们建议您在以下两个位置进行检查：主 Activity 的 onCreate() 方法中，及其 onResume() 方法中。
       * onCreate() 中的检查可确保该应用在检查成功之前无法使用。
       * onResume() 中的检查可确保当用户通过一些其他方式返回正在运行的应用（比如通过返回按钮）时，检查仍将继续进行。
       * 如果设备没有兼容的 Google Play 服务版本，您的应用可以调用以下方法，以便让用户从 Play 商店下载 Google Play 服务。
       * 它将尝试在此设备上提供Google Play服务。如果Play服务已经可用，则Task可以立即完成返回。
       */
      GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(context)

      // 或者使用以下代码
      /**
       * 通过isUserResolvableError来确定是否可以通过用户操作解决错误
       */
      if (GoogleApiAvailability.getInstance().isUserResolvableError(code)) {
        /**
         * 返回一个对话框，用于解决提供的errorCode。
         * @param activity  用于创建对话框的父活动
         * @param code      通过调用返回的错误代码
         * @param activity  调用startActivityForResult时给出的requestCode
         */
        GoogleApiAvailability.getInstance().getErrorDialog(context, code, 200)?.show()
      }
    }
    return false
  }
}

fun Activity.hasGms() = PlayUtil.playServiceExist(this)