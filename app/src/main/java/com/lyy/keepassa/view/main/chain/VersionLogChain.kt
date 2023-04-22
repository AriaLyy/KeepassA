/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.content.Context
import com.arialyy.frame.util.AndroidUtils
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.util.isDestroy
import com.lyy.keepassa.view.UpgradeLogDialog
import timber.log.Timber

/**
 * 显示版本日志对话框，显示逻辑：
 * 配置文件的版本号不存在，或当前版本号大于配置文件的版本号
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class VersionLogChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {
    val activity = chain.activity
    if (activity.isDestroy()) {
      Timber.i("activity 已经销毁")
      return MainDialogResponse(MainDialogResponse.RESPONSE_BREAK)
    }
    val sharedPreferences =
      activity.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val versionCode = sharedPreferences.getInt(Constance.VERSION_CODE, -1)
    if (versionCode < 0 || versionCode < AndroidUtils.getVersionCode(activity)) {
      UpgradeLogDialog().show()
      return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
    }
    return chain.proceed(activity)
  }

}