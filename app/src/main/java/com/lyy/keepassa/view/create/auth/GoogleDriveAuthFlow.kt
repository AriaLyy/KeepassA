/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.auth

import android.content.Context
import android.content.Intent
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.GoogleDriveUtil
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 16:12 2024/5/16
 **/
class GoogleDriveAuthFlow : IAuthFlow {
  private lateinit var context: Context
  private lateinit var callback: IAuthCallback

  override fun initContent(context: Context, callback: IAuthCallback) {
    this.context = context
    this.callback = callback
  }

  override fun startFlow() {
    // KpaUtil.scope.launch {
    //   GoogleDriveUtil.fileExists("/")
    // }
    // GoogleDriveUtil.auth()
  }

  override fun onResume() {
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
  }

  override fun onDestroy() {
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  }
}