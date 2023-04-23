/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.app.Activity
import android.content.Intent
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.search.AutoFillEntrySearchActivity

internal object OpenDbFinishDelegate : IAutoFillFinishDelegate {
  override fun finish(activity: BaseActivity<*>, autoFillParam: AutoFillParam) {
    /**
     * 打开搜索界面
     */
    val datas = KDBAutoFillRepository.getAutoFillDataByPackageName(autoFillParam.apkPkgName)
    // 如果查找不到数据，跳转到搜索页面
    if (datas == null || datas.isEmpty()) {
      AutoFillEntrySearchActivity.turnSearchActivity(
        activity, REQUEST_SEARCH_ENTRY_CODE,
        apkPkgName!!
      )
      return
    }

    // 将数据回调给service
    val data =
      KeepassAUtil.instance.getFillResponse(activity, activity.intent, autoFillParam.apkPkgName)
    activity.setResult(Activity.RESULT_OK, data)
  }

  override fun onActivityResult(activity: BaseActivity<*>, data: Intent?) {
    TODO("Not yet implemented")
  }
}