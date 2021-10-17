/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.R
import com.lyy.keepassa.view.detail.EntryDetailActivity
import com.lyy.keepassa.view.detail.EntryDetailActivity.Companion

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/10/17
 **/
interface ActivityRouter {

  /**
   * 跳转群组详情或项目详情
   */
  fun toEntryDetail(
    activity: FragmentActivity,
    entry: PwEntry,
    showElement: View? = null
  ) {
  }
}