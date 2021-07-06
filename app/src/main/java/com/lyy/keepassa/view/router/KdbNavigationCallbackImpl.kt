/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.router

import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.callback.NavigationCallback
import com.alibaba.android.arouter.launcher.ARouter

/**
 * @Author laoyuyu
 * @Description 数据库为空时，跳转启动页
 * @Date 2021/7/6
 **/
class KdbNavigationCallbackImpl : NavigationCallback {
  override fun onFound(postcard: Postcard?) {
  }

  override fun onLost(postcard: Postcard?) {
  }

  override fun onArrival(postcard: Postcard?) {
  }

  override fun onInterrupt(postcard: Postcard) {
    ARouter.getInstance()
      .build("/launcher/ac")
      .navigation()
  }
}