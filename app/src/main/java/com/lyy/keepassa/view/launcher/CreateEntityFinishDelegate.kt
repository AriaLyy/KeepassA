/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.app.ActivityOptions
import android.content.Intent
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.view.create.CreateEntryActivity
import com.lyy.keepassa.view.launcher.LauncherActivity.Companion

internal object CreateEntityFinishDelegate : IAutoFillFinishDelegate {
  override fun finish(activity: BaseActivity<*>, autoFillParam: AutoFillParam) {
    activity.startActivityForResult(
      Intent(activity, CreateEntryActivity::class.java).apply {
        putExtra(KEY_IS_AUTH_FORM_FILL_SAVE, true)
        putExtra(
          LauncherActivity.KEY_PKG_NAME,
          intent.getStringExtra(LauncherActivity.KEY_PKG_NAME)
        )
        putExtra(
          LauncherActivity.KEY_SAVE_USER_NAME,
          intent.getStringExtra(LauncherActivity.KEY_SAVE_USER_NAME)
        )
        putExtra(
          LauncherActivity.KEY_SAVE_PASS,
          intent.getStringExtra(LauncherActivity.KEY_SAVE_PASS)
        )
      }, REQUEST_SAVE_ENTRY_CODE, ActivityOptions.makeSceneTransitionAnimation(this)
        .toBundle()
    )
  }

  override fun onActivityResult(activity: BaseActivity<*>, data: Intent?) {
    TODO("Not yet implemented")
  }
}