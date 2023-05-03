/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
import android.os.Process
import com.blankj.utilcode.util.AppUtils
import com.lyy.keepassa.util.KeepassAUtil
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.tencent.vasdolly.helper.ChannelReaderUtil

object BuglyFeature : IFeature {
  override fun init(context: Context) {
    val kUtil = KeepassAUtil.instance
    // 获取当前包名
    val packageName: String = context.packageName

    // 获取当前进程名
    val processName = kUtil.getProcessName(Process.myPid())
    val strategy = UserStrategy(context)
    strategy.isUploadProcess = processName == null || processName == packageName
    strategy.appChannel = getChannel(context)
    strategy.appVersion = kUtil.getAppVersionName(context)
    CrashReport.initCrashReport(
      context.applicationContext, "59fc0ec759", AppUtils.isAppDebug(),
      strategy
    )
    //CrashReport.testJavaCrash();
  }

  private fun getChannel(context: Context): String {
    var channel = ChannelReaderUtil.getChannel(context.applicationContext)
    if (channel == null) {
      channel = "default"
    }
    return channel
  }
}