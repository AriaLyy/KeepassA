/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.util.PermissionsUtil
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class AutoFillPermissionsChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("AutoFillPermissionsChain")
    val ac = chain.activity
    if (!PermissionsUtil.needShowBackgroundStartDialog(ac)) {
      return chain.proceed(ac)
    }

    // showAutoFillMsgDialog 内部已处理 7 天冷静期;冷静期内返回 false,链继续
    val showed = PermissionsUtil.showAutoFillMsgDialog(
      ac,
      ResUtil.getString(R.string.hint_open_backgroun_start),
    )
    return if (showed) {
      MainDialogResponse(MainDialogResponse.RESPONSE_OK)
    } else {
      chain.proceed(ac)
    }
  }
}
