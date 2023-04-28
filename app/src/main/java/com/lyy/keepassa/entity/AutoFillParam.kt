/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 自动填充参数
 */
@Parcelize
data class AutoFillParam(
  val apkPkgName: String, // other apk packageName
  val domain: String? = null,
  val isSave: Boolean = false, // is save mode
  val saveUserName: String? = null, // User name at the of saving
  val savePass: String? = null  // Password name at the of saving
) : Parcelable
