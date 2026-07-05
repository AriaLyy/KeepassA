/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import android.content.Context
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.router.DialogRouter

/**
 * 已适配浏览器列表弹窗。按引擎分组、组内按展示名排序展示。
 */
object SupportedBrowsersDialog {

  @Suppress("UNUSED_PARAMETER")
  fun show(context: Context) {
    Routerfit.create(DialogRouter::class.java).showSupportedBrowsersDialog()
  }
}
