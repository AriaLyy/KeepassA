/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.lyy.keepassa.util.KdbUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:03 下午 2022/3/24
 **/
@Route(path = "/service/dbSave")
internal class KdbSaveService : IProvider {

  var scope = MainScope()
  val saveStateFlow = MutableSharedFlow<Int>()

  /**
   * save db by background
   */
  fun saveDbByBackground() {
    Timber.d("start save db by background")
    scope.launch(Dispatchers.IO) {
      val code = KdbUtil.saveDb(uploadDb = false)
      saveStateFlow.emit(code)
      Timber.d("save db complete, code = $code")
    }
  }

  /**
   * save db
   */
  fun saveDb() {

  }

  override fun init(context: Context?) {
  }
}