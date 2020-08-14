package com.lyy.keepassa.view

import androidx.annotation.DrawableRes
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp

enum class DbPathType(
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