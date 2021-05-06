/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.base

import android.app.Service
import android.content.Context
import com.lyy.keepassa.util.LanguageUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/5/6
 **/
abstract class BaseService:Service() {

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(LanguageUtil.setLanguage(newBase!!, BaseApp.currentLang))
  }
}