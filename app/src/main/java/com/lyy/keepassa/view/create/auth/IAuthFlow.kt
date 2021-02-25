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

interface IAuthCallback {
  fun callback(success: Boolean)
}

interface OnNextFinishCallback {
  fun onFinish(event: DbPathEvent)
}

object AuthFlowFactory {

  fun getAuthFlow(type: DbPathType): IAuthFlow? = when (type) {
    DROPBOX -> DropboxAuthFlow()
    AFS -> AFSAuthFlow()
    WEBDAV -> WebDavAuthFlow()
    else -> null
  }
}