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
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.ONE_DRIVE
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.create.CreateDbFirstFragment

/**
 * @Author laoyuyu
 * @Description cloud file create
 * @Date 2021/2/25
 **/
interface IAuthFlow {

  fun initContent(
    context: Context,
    callback: IAuthCallback
  )

  fun startFlow()

  fun onResume()

  /**
   * 点下一步的处理事件
   */
  fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  )

  fun onDestroy()

  fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  )
}

/**
 * 验证回调
 */
interface IAuthCallback {
  fun callback(success: Boolean)
}

/**
 * 完成选择云服务的回调
 */
interface OnNextFinishCallback {
  fun onFinish(event: DbPathEvent)
}

object AuthFlowFactory {

  fun getAuthFlow(type: DbPathType): IAuthFlow? = when (type) {
    DROPBOX -> DropboxAuthFlow()
    AFS -> AFSAuthFlow()
    WEBDAV -> WebDavAuthFlow()
    ONE_DRIVE -> OneDriveAuthFlow()
    else -> null
  }
}