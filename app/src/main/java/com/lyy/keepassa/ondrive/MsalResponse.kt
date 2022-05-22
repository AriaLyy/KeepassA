/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.ondrive

import androidx.annotation.Keep
import com.blankj.utilcode.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lyy.keepassa.R
import com.lyy.keepassa.util.cloud.OneDriveUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
@Keep
class MsalResponse<T> {
  @SerializedName("value")
  val value: T? = null
    get() {
      // https://docs.microsoft.com/zh-cn/graph/errors
      if (error?.code == "unauthenticated") {
        OneDriveUtil.initOneDrive {
          if (it) {
            OneDriveUtil.loadAccount()
            return@initOneDrive
          }
          ToastUtils.showLong(R.string.one_drive_init_failure)
        }
      }
      return field
    }

  @SerializedName("error")
  val error: MsalErrorInfo? = null
}

@Keep
data class MsalErrorInfo(
  val code: String, // https://docs.microsoft.com/zh-cn/graph/errors#code-property
  val message: String
) {
  override fun toString(): String {
    return Gson().toJson(this)
  }
}