/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 自动锁定数据库工具
 */
class AutoLockDbUtil private constructor() {
  private var requestTag = "LockDbWork"
  private val TIMER_TAG = "AutoLockDbTimer"
  private val timerGate = AutoLockTimerGate(CommonKvAutoLockLastStartTimeStorage)
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
    Timber.d( "resetTimer")
    startLockWorker()
  }

  /**
   * 用户仍在使用 app，刷新自动锁定计时。
   *
   * @return true 表示节流放行并已重新入队自动锁定 worker。
   */
  fun onUserActivity(): Boolean {
    if (!KeepassAUtil.instance.isAutoLockDb() || BaseApp.isLocked) {
      return false
    }
    return startLockWorker()
  }

  /**
   * cancel timer
   */
  fun cancelTimer(){
    manager.cancelAllWorkByTag(TIMER_TAG)
  }

  /**
   * 启动定时器
   */
  private fun startTimer(workRequest: OneTimeWorkRequest): Boolean {
    if (!timerGate.tryAcquire()) {
      return false
    }
    Timber.d( "开始自动锁定")

    // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/managing-work?hl=zh-cn
    // 唯一任务
    manager.enqueueUniqueWork(
        "autoLockDb",
        REPLACE, // 如果有新任务，则取消以前的任务
        workRequest
    )
    return true
  }

  /**
   * 立即启动定时器
   */
  fun startLockWorkerNow(): Boolean {
    val wordRequest = OneTimeWorkRequest.Builder(LockWorker::class.java)
        .addTag(requestTag)
        .build()
    return startTimer(wordRequest)
  }

  /**
   * 启动锁定数据库的工作线程
   */
  private fun startLockWorker(): Boolean {
    val time = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getString(BaseApp.APP.getString(R.string.set_key_auto_lock_db_time), "300")!!
        .toInt()
//    val time = 10
    val wordRequest = OneTimeWorkRequest.Builder(LockWorker::class.java)
        .addTag(TIMER_TAG)
        .setInitialDelay(time.toLong(), TimeUnit.SECONDS)
        .build()

    return startTimer(wordRequest)
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
      KeepassAUtil.instance.lockDb(DbLockTrigger.AUTO_LOCK)
      return Result.success()
    }

  }

}
