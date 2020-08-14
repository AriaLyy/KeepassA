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
    val TAG = StringUtil.getClassName(this)

    override fun doWork(): Result {
      val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
          .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)
      Log.d(TAG, "锁定数据库")
      BaseApp.isLocked = true
      // 只有应用在前台才会跳转到锁屏页面
      if (KeepassAUtil.isRunningForeground(BaseApp.APP) && BaseApp.KDB != null) {
        // 开启快速解锁则跳转到快速解锁页面
        if (isOpenQuickLock) {
          NotificationUtil.startQuickUnlockNotify(applicationContext)
          if (AbsFrame.getInstance().currentActivity is QuickUnlockActivity) {
            Log.w(TAG, "快速解锁已启动，不再启动快速解锁")
            return Result.success()
          }

          Log.d(TAG, "启动快速解锁")
          BaseApp.APP.startActivity(Intent(Intent.ACTION_MAIN).also {
            it.component =
              ComponentName(
                  BaseApp.APP.packageName,
                  "${BaseApp.APP.packageName}.view.main.QuickUnlockActivity"
              )
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          })
        } else {
          NotificationUtil.startDbLocked(applicationContext)
          // 没有开启快速解锁，则回到启动页
          if (AbsFrame.getInstance().currentActivity is LauncherActivity) {
            Log.w(TAG, "解锁页面已启动，不再启动快速解锁")
            return Result.success()
          }

          Log.d(TAG, "快速解锁没有启动，进入解锁界面")
          BaseApp.KDB.clear(applicationContext)
          BaseApp.KDB = null
          BaseApp.APP.startActivity(Intent(Intent.ACTION_MAIN).also {
            it.component =
              ComponentName(
                  BaseApp.APP.packageName,
                  "${BaseApp.APP.packageName}.view.launcher.LauncherActivity"
              )
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          })
          for (ac in AbsFrame.getInstance().activityStack) {
            if (ac is LauncherActivity) {
              continue
            }
            ac.finish()
          }
        }

      } else {
        if (isOpenQuickLock) {
          NotificationUtil.startQuickUnlockNotify(applicationContext)
        } else {
          NotificationUtil.startDbLocked(applicationContext)
        }
      }
      return Result.success()
    }

  }

}