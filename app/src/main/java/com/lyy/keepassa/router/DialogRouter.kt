/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.graphics.drawable.Drawable
import com.arialyy.frame.router.DialogArg
import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.dialog.CloudFileSelectDialog
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.view.dialog.TimeChangeDialog
import com.lyy.keepassa.view.dialog.WebDavLoginDialogNew

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/9/5
 **/
interface DialogRouter {

  @RouterPath(path = "/dialog/imgViewer")
  @DialogArg(showDialog = true)
  fun showImgViewerDialog(
    @RouterArgName(name = "imgByteArray") imgByteArray: ByteArray
  )

  @RouterPath(path = "/dialog/cloudFileList")
  fun getCloudFileListDialog(
    @RouterArgName(name = "storageType") storageType: StorageType,
    @RouterArgName(name = "onlyShowDir") onlyShowDir: Boolean = false
  ): CloudFileSelectDialog

  @RouterPath(path = "/dialog/cloudFileList")
  @DialogArg(showDialog = true)
  fun showCloudFileListDialog(
    @RouterArgName(name = "storageType") storageType: StorageType,
    @RouterArgName(name = "onlyShowDir") onlyShowDir: Boolean = false
  )

  @RouterPath(path = "/dialog/webdavLogin")
  fun getWebDavLoginDialog(): WebDavLoginDialogNew

  @RouterPath(path = "/dialog/webdavLogin")
  @DialogArg(showDialog = true)
  fun showWebDavLoginDialog(): WebDavLoginDialogNew

  @RouterPath(path = "/dialog/modifyGroup")
  @DialogArg(showDialog = true)
  fun showModifyGroupDialog(
    @RouterArgName(name = "pwGroup") pwGroup: PwGroupV4
  )

  @RouterPath(path = "/dialog/createGroup")
  @DialogArg(showDialog = true)
  fun showCreateGroupDialog(
    @RouterArgName(name = "parentGroup") parentGroup: PwGroupV4
  )

  @RouterPath(path = "/dialog/loading")
  @DialogArg(showDialog = false)
  fun getLoadingDialog(): LoadingDialog

  @RouterPath(path = "/dialog/loading")
  @DialogArg(showDialog = true)
  fun showLoadingDialog()

  /**
   * show play donate dialog
   */
  @RouterPath(path = "/dialog/playDonate")
  @DialogArg(showDialog = true)
  fun showPlayDonateDialog()

  /**
   * show display dialog
   * @param uuid don't use UUID, because is that Serializable
   */
  @RouterPath(path = "/dialog/totpDisplay")
  @DialogArg(showDialog = true)
  fun showTotpDisplayDialog(
    @RouterArgName(name = "uuid") uuid: String
  )

  /**
   * 显示消息对话框
   * @param showCountDownTimer 是否显示倒计时  Pair(true, 5) => 显示倒计时，5s
   */
  @RouterPath(path = "/dialog/msgDialog")
  @DialogArg(showDialog = true)
  fun showMsgDialog(
    @RouterArgName(name = "msgTitle", isObject = true) msgTitle: CharSequence = "",
    @RouterArgName(name = "msgContent", isObject = true) msgContent: CharSequence,
    @RouterArgName(name = "showCancelBt") showCancelBt: Boolean = true,
    @RouterArgName(name = "showEnterBt") showEnterBt: Boolean = true,
    @RouterArgName(name = "showCoverBt") showCoverBt: Boolean = false,
    @RouterArgName(name = "interceptBackKey") interceptBackKey: Boolean = false,
    @RouterArgName(name = "enterText", isObject = true) enterText: CharSequence = "",
    @RouterArgName(name = "cancelText", isObject = true) cancelText: CharSequence = "",
    @RouterArgName(name = "coverText", isObject = true) coverText: CharSequence = "",
    @RouterArgName(name = "enterBtTextColor") enterBtTextColor: Int = R.color.text_blue_color,
    @RouterArgName(name = "cancelBtTextColor") cancelBtTextColor: Int = R.color.text_gray_color,
    @RouterArgName(name = "coverBtTextColor") coverBtTextColor: Int = R.color.text_blue_color,
    @RouterArgName(
      name = "btnClickListener",
      isObject = true
    ) btnClickListener: OnMsgBtClickListener? = null,
    @RouterArgName(name = "msgTitleEndIcon", isObject = true) msgTitleEndIcon: Drawable? = null,
    @RouterArgName(name = "msgTitleStartIcon", isObject = true) msgTitleStartIcon: Drawable? = null,
    @RouterArgName(name = "showCountDownTimer") showCountDownTimer: Pair<Boolean, Int> = Pair(
      false,
      5
    )
  )

  /**
   * 日期选择对话框
   */
  @RouterPath(path = "/dialog/timeChange")
  fun getTimeChangeDialog(): TimeChangeDialog
}