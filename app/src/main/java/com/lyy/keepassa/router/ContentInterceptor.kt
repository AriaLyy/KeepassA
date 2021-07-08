/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.content.Context
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Interceptor
import com.alibaba.android.arouter.facade.callback.InterceptorCallback
import com.alibaba.android.arouter.facade.template.IInterceptor
import com.alibaba.android.arouter.launcher.ARouter
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.KdbUtil.isNull
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:14 下午 2021/7/7
 **/
@Interceptor(priority = 8, name = "ContentInterceptor")
class ContentInterceptor : IInterceptor {
  override fun init(context: Context) {
    // 拦截器的初始化，会在sdk初始化的时候调用该方法，仅会调用一次
  }

  override fun process(
    postcard: Postcard,
    callback: InterceptorCallback
  ) {
    Timber.d("route path => ${postcard.path}")
    if (postcard.path == "/launcher/activity"){
      callback.onContinue(postcard)
      return
    }
    if (BaseApp.KDB.isNull()) {
      callback.onInterrupt(Exception("kdb is null"))
      ARouter.getInstance()
        .build("/launcher/activity")
        .navigation()
      return
    }
    if (BaseApp.isLocked && BaseApp.dbRecord != null) {
      callback.onInterrupt(Exception("database is locked"))
      return
    }


    callback.onContinue(postcard)  // 处理完成，交还控制权
    // callback.onInterrupt(new RuntimeException("我觉得有点异常"));      // 觉得有问题，中断路由流程

    // 以上两种至少需要调用其中一种，否则不会继续路由
  }
}