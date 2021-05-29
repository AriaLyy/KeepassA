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
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.cloud.OneDriveUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.dialog.CloudFileListDialog
import com.lyy.keepassa.view.dialog.MsgDialog

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
    val title = ResUtil.getString(R.string.hint)
    val content = Html.fromHtml(activity!!.resources.getString(R.string.one_drive_hint))
    val msgDialog = MsgDialog.generate {
      msgTitle = title
      msgContent = content
      build()
    }
    msgDialog.setOnBtClickListener(object : MsgDialog.OnBtClickListener {
      override fun onBtClick(type: Int, view: View) {
        onClick.invoke()
      }
    })
    msgDialog.show()
  }

  /**
   * 显示云端文件列表
   */
  private fun showCloudListDialog() {
    val dialog = CloudFileListDialog().apply {
      putArgument("cloudFileDbPathType", ONE_DRIVE)
    }
    dialog.show(activity!!.supportFragmentManager, "cloud_file_list_dialog")
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