/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.view.dialog.DonateDialog
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import org.joda.time.DateTime

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class DevBirthdayChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {
//    val dt = DateTime(2020, 10, 2, 0, 0)
    val dt = DateTime(System.currentTimeMillis())
    if (dt.monthOfYear == 10 && dt.dayOfMonth == 2) {
      Routerfit.create(DialogRouter::class.java).showMsgDialog(
        msgTitle = ResUtil.getString(R.string.donate),
        msgContent = ResUtil.getString(R.string.dev_birthday),
        cancelText = "NO",
        enterText = "YES",
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            DonateDialog().show()
          }

          override fun onCancel(v: Button) {
          }
        }
      )
      return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
    }
    return chain.proceed(chain.activity)
  }
}