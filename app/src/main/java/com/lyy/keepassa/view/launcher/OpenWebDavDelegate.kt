/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.lyy.keepassa.view.dialog.WebDavLoginDialog

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
class OpenWebDavDelegate : IOpenDbDelegate {

  override fun onResume() {
  }

  override fun startFlow(fragment: ChangeDbFragment) {
    val dialog = WebDavLoginDialog()
    dialog.show(fragment.childFragmentManager, "web_dav_login")
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }

  override fun destroy() {
  }
}