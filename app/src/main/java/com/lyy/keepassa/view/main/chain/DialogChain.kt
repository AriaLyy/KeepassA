/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import com.lyy.keepassa.view.main.MainActivity

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class DialogChain(
  val activity: MainActivity,
  private val interceptors: List<IMainDialogInterceptor>,
  private val index: Int = 0
) : IMainDialogInterceptor.IChain {

  override fun proceed(context: MainActivity): MainDialogResponse {
    if (index > interceptors.size - 1) {
      return MainDialogResponse(MainDialogResponse.RESPONSE_FINISH)
    }
    val interceptor = interceptors[index]
    val next = DialogChain(activity, interceptors, index + 1)
    return interceptor.intercept(next)
  }

}