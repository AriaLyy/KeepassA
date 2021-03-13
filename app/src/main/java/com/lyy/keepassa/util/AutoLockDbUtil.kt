/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import java.util.concurrent.TimeUnit

/**
 * 自动锁定数据库工具
 */
class AutoLockDbUtil private constructor() {
  private var requestTag = "LockDbWork"
  private val KEY_NAME = "LockTimer"
  private val KEY_LAST_START_TIME = "LastStartTime"
  private val TAG = StringUtil.getClassName(this)
  private val sp = BaseApp.APP.getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
  private val manager by lazy {
    WorkManager.getInstance(BaseApp.APP)
  }

  companion object {
    private val instance: AutoLockDbUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      AutoLockDbUtil()
    }

    fun get(): AutoLockDbUtil {
      return instance
    }
  }

  /**
   * 重置定时器
   */
  fun resetTimer() {
    KLog.d(TAG, "resetTimer")
    startLockWorker()
  }

  /**
   * 启动定时器
   */
  private fun startTimer(workRequest: OneTimeWorkRequest) {
    val lastStartTime = sp.getLong(KEY_LAST_START_TIME, -1)
    if (lastStartTime > 0 && System.currentTimeMillis() - lastStartTime <= 3000) {
      return
    }
    Log.d(TAG, "开始自动锁定")
    sp.edit(true) {
      putLong(KEY_LAST_START_TIME, System.currentTimeMillis())
    }

    // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/managing-work?hl=zh-cn
    // 唯一任务
    manager.enqueueUniqueWork(
        "autoLockDb",
        REPLACE, // 如果有新任务，则取消以前的任务
        workRequest
    )
  }

  /**
   * 立即启动定时器
   */
  fun startLockWorkerNow() {
    val wordRequest = OneTimeWorkRequest.Builder(LockWorker::class.java)
        .addTag(requestTag)
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
  ) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
      KeepassAUtil.instance.lock()
      return Result.success()
    }

  }

}