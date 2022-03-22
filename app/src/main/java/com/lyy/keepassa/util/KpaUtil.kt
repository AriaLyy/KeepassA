/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2022/3/22
 **/
object KpaUtil {
  var scope = MainScope()

  /**
   * save db by background
   */
  fun saveDbByBackground() {
    Timber.d("start save db by background")
    scope.launch(Dispatchers.IO) {
      val code = KdbUtil.saveDb(uploadDb = false)
      Timber.d("save db complete, code = $code")

    }
  }
}