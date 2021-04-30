/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import androidx.annotation.DrawableRes
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp

enum class StorageType(
  var type: Int,
  @DrawableRes var icon: Int,
  var lable: String
) {
  AFS(0, R.drawable.ic_android, BaseApp.APP.getString(R.string.afs)),
  DROPBOX(1, R.drawable.ic_dropbox, "Dropbox"),
  ONE_DRIVE(2, R.drawable.ic_onedrive, "OneDrive"),
  GOOGLE_DRIVE(3, R.drawable.ic_google_drive, "GoogleDrive"),
  WEBDAV(4, R.drawable.ic_http, "WebDav"),
  FTP(5, R.drawable.ic_ftp, "Ftp"),
  UNKNOWN(-1, R.drawable.ic_android, "Unknown")
}