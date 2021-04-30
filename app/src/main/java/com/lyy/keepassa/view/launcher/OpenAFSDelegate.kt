/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.StorageType.AFS
import org.greenrobot.eventbus.EventBus

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
class OpenAFSDelegate : IOpenDbDelegate {
  private val REQ_CODE_OPEN_DB_BY_AFS = 0xa1
  private var activity: FragmentActivity? = null

  override fun onResume() {

  }

  override fun startFlow(fragment: ChangeDbFragment) {
    this.activity = fragment.requireActivity()
    KeepassAUtil.instance.openSysFileManager(
        fragment, "*/*", REQ_CODE_OPEN_DB_BY_AFS
    )
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == REQ_CODE_OPEN_DB_BY_AFS && data != null && data.data != null) {
        // 申请长期的uri权限
        data.data!!.takePermission()
        EventBus.getDefault()
            .post(
                ChangeDbEvent(
                    dbName = UriUtil.getFileNameFromUri(activity, data.data),
                    localFileUri = data.data!!,
                    uriType = AFS
                )
            )
      }
    }
  }

  override fun destroy() {
    activity = null
  }
}