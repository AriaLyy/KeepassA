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
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.arialyy.frame.router.Routerfit
import com.dropbox.core.android.Auth
import com.lyy.keepassa.R
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.dialog.CloudFileListDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener

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
      Routerfit.create(DialogRouter::class.java).showMsgDialog(
        msgContent = Html.fromHtml(delegateRequireActivity().getString(R.string.dropbox_msg)),
        showCancelBt = true,
        interceptBackKey = true,
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            Auth.startOAuth2Authentication(delegateRequireActivity(), DropboxUtil.APP_KEY)
          }

          override fun onCancel(v: Button) {
            startDropboxAuth = false
          }

        }
      )
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