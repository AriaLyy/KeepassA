/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.launcher

import android.content.Intent
import android.text.Html
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.OneDriveUtil
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
class OpenOneDriveDelegate : IOpenDbDelegate {
  private var activity: FragmentActivity? = null

  override fun onResume() {
  }

  override fun startFlow(fragment: ChangeDbFragment) {
    this.activity = fragment.requireActivity()
    showHitDialog {
      if (KpaUtil.isChina()) {
        ToastUtils.showLong(ResUtil.getString(R.string.please_open_proxy))
      }
      OneDriveUtil.initOneDrive { success ->
        if (success) {
          OneDriveUtil.loadAccount()
          return@initOneDrive
        }
        HitUtil.snackLong(
          activity!!.window.decorView,
          activity!!.getString(R.string.one_drive_init_failure)
        )
      }
      OneDriveUtil.loginCallback = object : OneDriveUtil.OnLoginCallback {
        override fun callback(success: Boolean) {
          if (success) {
            showCloudListDialog()
          }
        }
      }
    }
  }

  private fun showHitDialog(onClick: () -> Unit) {
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = Html.fromHtml(activity!!.resources.getString(R.string.one_drive_hint)),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onCover(v: Button) {
        }

        override fun onEnter(v: Button) {
          onClick.invoke()
        }

        override fun onCancel(v: Button) {
        }
      }
    )
  }

  /**
   * 显示云端文件列表
   */
  private fun showCloudListDialog() {
    Routerfit.create(DialogRouter::class.java).showCloudFileListDialog(ONE_DRIVE)
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }

  override fun destroy() {
    OneDriveUtil.loginCallback = null
    activity = null
  }
}