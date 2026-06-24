/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

// import com.lyy.keepassa.util.cloud.GoogleDriveUtil
import android.content.Intent
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.GoogleDriveUtil
import com.lyy.keepassa.util.cloud.GoogleDriveUtil.AUTH_STATE_FLOW
import com.lyy.keepassa.view.StorageType.GOOGLE_DRIVE
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 17:25 2024/5/16
 **/
object OpenGoogleDriveDelegate : IOpenDbDelegate {
  private var isAuthed = false
  private val listDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getCloudFileListDialog(GOOGLE_DRIVE)
  }

  private var authJob: Job? = null

  override fun startFlow(fragment: ChangeDbFragment) {
    GoogleDriveUtil.auth()
    if (authJob == null || authJob?.isActive == false) {
      authJob = KpaUtil.scope.launch {
        AUTH_STATE_FLOW.collectLatest {
          if (it) {
            isAuthed = true
            Timber.d("auth success")
            return@collectLatest
          }
        }
      }
    }
  }

  override fun onResume() {
    if (!isAuthed) {
      return
    }
    listDialog.show()
    // GoogleDriveUtil.chooseFile()
    // reset state
    isAuthed = false
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  }

  override fun destroy() {
    authJob?.cancel()
  }
}