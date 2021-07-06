/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.router

import android.content.Context
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Interceptor
import com.alibaba.android.arouter.facade.callback.InterceptorCallback
import com.alibaba.android.arouter.facade.template.IInterceptor
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.KdbUtil.isNull

/**
 * 拦截器会在跳转之间执行，多个拦截器会按优先级顺序依次执行
 * @Author laoyuyu
 * @Description  Arouter interceptor, if the database is empty, jump to the startup page
 * @Date 2021/7/6
 **/
@Interceptor(priority = 8, name = "loginInterceptor")
class LoginInterceptor : IInterceptor {
  override fun init(context: Context) {
    // 拦截器的初始化，会在sdk初始化的时候调用该方法，仅会调用一次
  }

  override fun process(postcard: Postcard, callback: InterceptorCallback) {
    if (BaseApp.KDB.isNull() || BaseApp.isLocked){
      callback.onInterrupt(null)

      return
    }
    callback.onContinue(postcard) // 处理完成，交还控制权
    // callback.onInterrupt(new RuntimeException("我觉得有点异常"));      // 觉得有问题，中断路由流程

    // 以上两种至少需要调用其中一种，否则不会继续路由
  }
}