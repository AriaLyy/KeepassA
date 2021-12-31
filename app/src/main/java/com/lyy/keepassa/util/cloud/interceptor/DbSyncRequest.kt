/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud.interceptor

import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.cloud.ICloudUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:41 下午 2021/12/24
 **/
class DbSyncRequest constructor(
  var record: DbHistoryRecord,
  var syncUtil: ICloudUtil,
  val interceptors: List<IDbSyncInterceptor>,
  val index: Int = 0,
) {

  fun nextInterceptor(): IDbSyncInterceptor? {
    if (index == interceptors.size) {
      return null
    }
    return interceptors[index + 1]
  }
}