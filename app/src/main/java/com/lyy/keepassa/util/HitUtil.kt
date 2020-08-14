package com.lyy.keepassa.util

import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.keepassdroid.database.exception.InvalidDBException
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
    when (e) {
      is InvalidDBException -> {
        toaskShort(BaseApp.APP.getString(R.string.error_open_db))
      }
      is FileNotFoundException -> {
        toaskShort(BaseApp.APP.getString(R.string.db_file_no_exist))
      }
      is SardineException -> {
        when (e.statusCode) {
          HttpURLConnection.HTTP_UNAUTHORIZED -> {
            toaskShort(BaseApp.APP.getString(R.string.invalid_auth))
          }
          HttpURLConnection.HTTP_NOT_FOUND -> {
            toaskShort(BaseApp.APP.getString(R.string.db_file_no_exist))
          }
        }
      }
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
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).setAction("OK") {}.show()
  }

  fun snackLong(
    view: View,
    text: String
  ) {
    Snackbar.make(view, text, Snackbar.LENGTH_LONG).setAction("OK") {}.show()
  }

}