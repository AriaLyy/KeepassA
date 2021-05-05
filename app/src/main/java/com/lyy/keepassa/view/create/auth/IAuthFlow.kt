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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.AFS
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.StorageType.WEBDAV
import com.lyy.keepassa.view.create.CreateDbFirstFragment

/**
 * @Author laoyuyu
 * @Description cloud file create
 * @Date 2021/2/25
 **/
interface IAuthFlow : LifecycleObserver {

  fun initContent(
    context: Context,
    callback: IAuthCallback
  )

  fun startFlow()

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
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

  fun getAuthFlow(type: StorageType): IAuthFlow? = when (type) {
    DROPBOX -> DropboxAuthFlow()
    AFS -> AFSAuthFlow()
    WEBDAV -> WebDavAuthFlow()
    ONE_DRIVE -> OneDriveAuthFlow()
    else -> null
  }
}