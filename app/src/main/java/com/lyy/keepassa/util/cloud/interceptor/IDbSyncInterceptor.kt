/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud.interceptor

import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:34 下午 2021/12/24
 **/
interface IDbSyncInterceptor {

  suspend fun intercept(request: DbSyncRequest): DbSyncResponse

  fun error(code: Int, msg: String): DbSyncResponse {
    Timber.e(msg)
    return DbSyncResponse(code, msg)
  }

  fun normal(code: Int, msg: String): DbSyncResponse {
    Timber.i(msg)
    return DbSyncResponse(code, msg)
  }
}