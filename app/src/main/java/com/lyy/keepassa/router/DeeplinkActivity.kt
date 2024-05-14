/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arialyy.frame.router.Routerfit
import timber.log.Timber
import java.net.URLDecoder

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/7/11
 **/
class DeeplinkActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.d("uri = ${intent.data}")
    val shortcutData = intent.getStringExtra("shortcutData")
    if (!shortcutData.isNullOrEmpty()) {
      Timber.d("shortcutData = $shortcutData")
      val uriString = URLDecoder.decode(shortcutData)
      val uri = Uri.parse(uriString)
      handleFormShortcutRoute(uri)
      finish()
      return
    }
  }

  private fun handleFormShortcutRoute(uri: Uri) {
    val ac = uri.getQueryParameter("ac")
    if (ac == "createEntry") {
      Timber.d("to create entry")
      Routerfit.create(ActivityRouter::class.java).toCreateEntryActivity(
        groupId = null,
        isFromShortcuts = true
      )
      return
    }
    if (ac == "search") {
      val type = uri.getQueryParameter("shortcutsType")
      Timber.d("to search ac, type: $type")
      Routerfit.create(ActivityRouter::class.java).toMainActivity(true, type!!.toInt())
    }
  }
}