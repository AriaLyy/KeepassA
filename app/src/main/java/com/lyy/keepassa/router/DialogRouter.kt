/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.router

import android.graphics.drawable.Drawable
import com.arialyy.frame.router.RouterArgName
import com.arialyy.frame.router.RouterPath
import com.lyy.keepassa.R
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/9/5
 **/
interface DialogRouter {

  /**
   * 显示消息对话框
   * @param showCountDownTimer 是否显示倒计时  Pair(true, 5) => 显示倒计时，5s
   */
  @RouterPath(path = "/dialog/msgDialog")
  fun toMsgDialog(
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
    @RouterArgName(name = "btnClickListener", isObject = true) btnClickListener: OnMsgBtClickListener? = null,
    @RouterArgName(name = "msgTitleEndIcon", isObject = true) msgTitleEndIcon: Drawable? = null,
    @RouterArgName(name = "msgTitleStartIcon", isObject = true) msgTitleStartIcon: Drawable? = null,
    @RouterArgName(name = "showCountDownTimer") showCountDownTimer: Pair<Boolean, Int> = Pair(
      false,
      5
    )
  ): MsgDialog
}