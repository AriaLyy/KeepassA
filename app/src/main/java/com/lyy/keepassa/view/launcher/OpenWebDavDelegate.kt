/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.event.WebDavLoginEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.view.StorageType.WEBDAV
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
class OpenWebDavDelegate : IOpenDbDelegate {
  private val scope = MainScope()
  private var loginEvent: WebDavLoginEvent? = null

  override fun onResume() {
  }

  override fun startFlow(fragment: ChangeDbFragment) {
    val dialog = Routerfit.create(DialogRouter::class.java).getWebDavLoginDialog()
    dialog.show(fragment.childFragmentManager, "web_dav_login")
    scope.launch {
      dialog.webDavLoginFlow.collectLatest {
        if (it.loginSuccess) {
          loginEvent = it
          Routerfit.create(DialogRouter::class.java).showCloudFileListDialog(WEBDAV)
        }
      }
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }

  override fun destroy() {
    scope.cancel()
  }
}