/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.nofreelib

import android.content.Context
import com.lyy.keepassa.baseapi.INotFreeLibService
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy

/**
 * bugly 配置
 */
object BuglyConfig : INotFreeLibService {

  override fun initLib(
    context: Context,
    isDebug: Boolean,
    channel: String,
    other: String?
  ) {
    // 用户控制
    CrashReport.setIsDevelopmentDevice(context, isDebug) // 测试不打开

    val strategy = UserStrategy(context)
    strategy.appChannel = channel
    CrashReport.initCrashReport(
        context.applicationContext, "59fc0ec759",
        isDebug,
        strategy
    )
  }
}