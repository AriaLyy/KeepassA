/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.content.Context
import androidx.core.content.edit
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.view.dialog.DonateDialog

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class DonateChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {
    val ac = chain.activity
    val pre = ac.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val startNum = pre.getInt(Constance.PRE_KEY_START_APP_NUM, 0)
    if (startNum >= Constance.START_DONATE_JUDGMENT_VALUE) {
      val donateDialog = DonateDialog()
      donateDialog.setOnDismissListener {
        pre.edit {
          putInt(Constance.PRE_KEY_START_APP_NUM, 0)
        }
      }
      donateDialog.show()
      return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
    }
    return chain.proceed(ac)
  }
}