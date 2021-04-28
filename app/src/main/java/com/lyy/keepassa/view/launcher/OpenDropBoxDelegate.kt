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
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.dropbox.core.android.Auth
import com.lyy.keepassa.R
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.dialog.CloudFileListDialog
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.MsgDialog.OnBtClickListener

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/25
 **/
class OpenDropBoxDelegate : IOpenDbDelegate {
  private val TAG = javaClass.simpleName
  private var startDropboxAuth = false
  private var activity: FragmentActivity? = null

  override fun onResume() {
    if (startDropboxAuth) {
      val token = Auth.getOAuth2Token()
      if (!TextUtils.isEmpty(token)) {
        DropboxUtil.saveToken(token)
        // 如果授权成功，进入下一步
        showCloudListDialog()
      } else {
        HitUtil.toaskShort(
            "dropbox ${delegateRequireActivity().getString(R.string.auth)}${
              delegateRequireActivity().getString(
                  R.string.fail
              )
            }"
        )
      }
    }
  }

  override fun startFlow(fragment: ChangeDbFragment) {
    this.activity = fragment.requireActivity()
    changeDropbox()
  }

  private fun delegateRequireActivity(): FragmentActivity = activity!!

  /**
   * 显示云端文件列表
   */
  private fun showCloudListDialog() {
    val dialog = CloudFileListDialog().apply {
      putArgument("cloudFileDbPathType", DROPBOX)
    }
    dialog.show(delegateRequireActivity().supportFragmentManager, "cloud_file_list_dialog")
  }

  /**
   * 选择dropbox数据库
   */
  private fun changeDropbox() {
    if (DropboxUtil.isAuthorized()) {
      showCloudListDialog()
    } else {
      startDropboxAuth = true
      val title = delegateRequireActivity().getString(R.string.hint)
      val msgDialog = MsgDialog.generate {
        msgTitle = title
        msgContent = Html.fromHtml(delegateRequireActivity().getString(R.string.dropbox_msg))
        showCancelBt = true
        interceptBackKey = true
        build()
      }
      msgDialog.setOnBtClickListener(object : OnBtClickListener {
        override fun onBtClick(
          type: Int,
          view: View
        ) {
          if (type == MsgDialog.TYPE_ENTER) {
            Auth.startOAuth2Authentication(delegateRequireActivity(), DropboxUtil.APP_KEY)
          } else {
            startDropboxAuth = false
          }
        }
      })
      msgDialog.show()
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }

  override fun destroy() {
    activity = null
  }
}