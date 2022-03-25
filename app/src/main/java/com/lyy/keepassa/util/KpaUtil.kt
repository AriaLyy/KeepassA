/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.router.ServiceRouter
import kotlinx.coroutines.MainScope

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/3/22
 **/
object KpaUtil {
  var scope = MainScope()
  val kdbService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbSaveService()
  }
}