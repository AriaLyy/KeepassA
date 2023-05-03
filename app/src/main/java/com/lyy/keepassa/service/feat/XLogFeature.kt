/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
import com.blankj.utilcode.util.Utils
import com.lyy.keepassa.BuildConfig
import com.tencent.mars.xlog.Log
import com.tencent.mars.xlog.Xlog
import com.tencent.mars.xlog.Xlog.XLogConfig
import timber.log.Timber
import timber.log.Timber.DebugTree

object XLogFeature : IFeature {

  init {
    System.loadLibrary("c++_shared")
    System.loadLibrary("marsxlog")
  }

  private val rootDir: String = Utils.getApp().filesDir.absolutePath
  private val logDir = "$rootDir/marssample/log"
  private val cacheDir = "$rootDir/marssample/cache"

  override fun init(context: Context) {
    val xlog = Xlog()
    Log.setLogImp(xlog)
    Log.setConsoleLogOpen(false)
    Log.appenderOpen(Xlog.LEVEL_VERBOSE, Xlog.AppednerModeAsync, cacheDir, logDir, "kpa", 0)
    setTimberPlant()
  }

  fun flush() {
    Timber.d("写日志到xlog中")
    Log.appenderFlush()
  }

  private fun setTimberPlant() {
    Timber.plant(object : DebugTree() {
      override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (BuildConfig.DEBUG) {
          return
        }
        when (priority) {
          android.util.Log.DEBUG -> {
            Log.d(tag, message)
          }
          android.util.Log.VERBOSE -> {
            Log.v(tag, message)
          }
          android.util.Log.WARN -> {
            Log.w(tag, message)
          }
          android.util.Log.INFO -> {
            Log.i(tag, message)
          }
          android.util.Log.ERROR -> {
            Log.e(tag, message)
          }
          android.util.Log.ASSERT -> {
            Log.f(tag, message)
          }
        }
      }
    })
  }
}