/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.keepassdroid.database.exception.ArcFourException
import com.keepassdroid.database.exception.InvalidAlgorithmException
import com.keepassdroid.database.exception.InvalidDBException
import com.keepassdroid.database.exception.InvalidDBSignatureException
import com.keepassdroid.database.exception.InvalidDBVersionException
import com.keepassdroid.database.exception.InvalidKeyFileException
import com.keepassdroid.database.exception.InvalidPasswordException
import com.keepassdroid.database.exception.KeyFileEmptyException
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import java.io.FileNotFoundException
import java.net.HttpURLConnection

/**
 * 提示工具
 */
object HitUtil {

  /**
   * 打印打开数据库的错误提示
   */
  fun toaskOpenDbException(e: Exception) {
    e.printStackTrace()
    when (e) {
      is ArcFourException -> {
        toaskShort(R.string.error_open_db_arcfour_error)
      }
      is InvalidAlgorithmException -> {
        toaskShort(R.string.error_open_db_algorithm_error)
      }
      is InvalidDBSignatureException -> {
        toaskShort(R.string.error_open_db_signature_error)
      }
      is InvalidDBVersionException -> {
        toaskShort(R.string.error_open_db_version_error)
      }
      is InvalidKeyFileException -> {
        toaskShort(R.string.error_open_db_key_invalid)
      }
      is InvalidPasswordException -> {
        toaskShort(R.string.error_open_db_pass_error)
      }
      is KeyFileEmptyException -> {
        toaskShort(R.string.error_open_db_key_empty)
      }
      is InvalidDBException -> {
        toaskShort(R.string.error_open_db)
      }
      is FileNotFoundException -> {
        toaskShort(R.string.db_file_no_exist)
      }
      is SardineException -> {
        when (e.statusCode) {
          HttpURLConnection.HTTP_UNAUTHORIZED -> {
            toaskShort(R.string.invalid_auth)
          }
          HttpURLConnection.HTTP_NOT_FOUND -> {
            toaskShort(R.string.db_file_no_exist)
          }
        }
      }
    }
  }

  fun toaskShort(@StringRes strId: Int) {
    BaseApp.handler.post {
      Toast.makeText(BaseApp.APP, BaseApp.APP.resources.getString(strId), Toast.LENGTH_SHORT)
        .show()
    }
  }

  fun toaskShort(text: String) {
    BaseApp.handler.post {
      Toast.makeText(BaseApp.APP, text, Toast.LENGTH_SHORT)
        .show()
    }
  }

  fun toaskLong(text: String) {
    BaseApp.handler.post {
      Toast.makeText(BaseApp.APP, text, Toast.LENGTH_LONG)
        .show()
    }
  }

  fun snackShort(
    view: View,
    text: String
  ) {
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
      .setAction("OK") {}
      .show()
  }

  fun snackLong(
    view: View,
    text: String
  ) {
    Snackbar.make(view, text, Snackbar.LENGTH_LONG)
      .setAction("OK") {}
      .show()
  }

  fun snackLong(
    view: View,
    text: String,
    actionStr: String,
    action: View.OnClickListener
  ) {
    Snackbar.make(view, text, Snackbar.LENGTH_LONG)
      .setAction(actionStr, action)
      .show()
  }
}