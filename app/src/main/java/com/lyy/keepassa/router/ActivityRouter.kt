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
import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.keepassdroid.database.PwEntry

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/10/17
 **/
interface ActivityRouter {

  @RouterPath(path = "/main/ac")
  fun toMainActivity(
    @RouterArgName(name = "isShortcuts") isShortcuts: Boolean = false,
    @RouterArgName(name = "shortcutType") shortcutType: Int = 1,
    @RouterArgName(name = "opt") opt: ActivityOptionsCompat? = null
  )

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