/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.util.SharePreUtil
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 自动锁定数据库工具
 */
class AutoLockDbUtil private constructor() {
  private var timerId: UUID? = null
  private val KEY_NAME = "LockTimer"
  private val TAG = StringUtil.getClassName(this)

  companion object {
    private var instance: AutoLockDbUtil? = null
      get() {
        if (field == null) {
          field = AutoLockDbUtil()
        }
        return field
      }

    //细心的小伙伴肯定发现了，这里不用getInstance作为为方法名，是因为在伴生对象声明时，内部已有getInstance方法，所以只能取其他名字
    fun get(): AutoLockDbUtil {

      return instance!!
    }
  }

  /**
   * 重置定时器
   */
  fun resetTimer() {
    cancelTimer()
    startLockWorker()
  }

  /**
   * 启动定时器
   */
  private fun startTimer(workRequest: WorkRequest) {
    timerId = workRequest.id
    SharePreUtil.putString(Constance.PRE_FILE_NAME, BaseApp.APP, KEY_NAME, timerId.toString())
    WorkManager.getInstance(BaseApp.APP)
        .enqueue(workRequest)
  }

  /**
   * 取消定时
   */
  fun cancelTimer() {
    if (timerId != null) {
      WorkManager.getInstance(BaseApp.APP)
          .cancelWorkById(timerId!!)
    } else {
      val temp = SharePreUtil.getString(Constance.PRE_FILE_NAME, BaseApp.APP, KEY_NAME)
      if (temp != null && temp.isNotEmpty()) {
        timerId = UUID.fromString(temp)
        WorkManager.getInstance(BaseApp.APP)
            .cancelWorkById(timerId!!)
      } else {
        WorkManager.getInstance(BaseApp.APP)
            .cancelAllWork()
      }
    }
  }

  /**
   * 立即启动定时器
   */
  fun startLockWorkerNow() {
    val wordRequest = OneTimeWorkRequest.Builder(LockWorker::class.java)
        .build()
    startTimer(wordRequest)
  }

  /**
   * 启动锁定数据库的工作线程
   */
  private fun startLockWorker() {
    val time = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getString(BaseApp.APP.getString(R.string.set_key_auto_lock_db_time), "300")!!
        .toInt()
//    val time = 10
    Log.d(TAG, "自动锁定时间：${time}秒")
    val wordRequest = OneTimeWorkRequest.Builder(LockWorker::class.java)
        .setInitialDelay(time.toLong(), TimeUnit.SECONDS)
        .build()

    startTimer(wordRequest)
  }

  /**
   * 锁定数据库线程任务
   * 如果开启了快速解锁，进入快速解锁界面
   * 如果没有开启快速解锁，直接进入启动页，并清空数据库
   */
  class LockWorker(
    appContext: Context,
    workerParams: WorkerParameters
  ) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
      KeepassAUtil.instance.lock()
      return Result.success()
    }

  }

}