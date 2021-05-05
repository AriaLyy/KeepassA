/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.ondrive

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
@Keep
data class MsalResponse<T>(
  @SerializedName("value")
  val value: T?,

  @SerializedName("error")
  val error: MsalErrorInfo?
)

@Keep
data class MsalErrorInfo(
  val code: String,
  val message: String
){
  override fun toString(): String {
    return Gson().toJson(this)
  }
}