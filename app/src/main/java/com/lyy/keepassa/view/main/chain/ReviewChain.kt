/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.content.Context
import com.blankj.utilcode.util.ActivityUtils
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.ReviewBean
import com.lyy.keepassa.util.CommonKVStorage
import com.lyy.keepassa.util.hasGms
import com.lyy.keepassa.view.dialog.ReviewDialog
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2024/5/8
 **/
class ReviewChain : IMainDialogInterceptor {
  private val START_REVIEW_NUM = 100

  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("review")
    val ac = chain.activity
    val pre = ac.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
    val startNum = pre.getInt(Constance.PRE_KEY_START_APP_NUM, 0)

    val reviewBean =
      CommonKVStorage.get(Constance.KEY_REVIEW, ReviewBean::class.java, null) ?: ReviewBean(false)

    if (startNum % START_REVIEW_NUM == 0
      && !reviewBean.isAlreadyReview
      && ActivityUtils.getTopActivity().hasGms()
    ) {
      ReviewDialog().show()
      return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
    }

    return chain.proceed(ac)
  }
}