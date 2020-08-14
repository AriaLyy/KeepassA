package com.lyy.keepassa.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import java.util.concurrent.TimeUnit

/**
 * 剪切板工具
 */
class ClipboardUtil private constructor() {
  private val clipManager: ClipboardManager =
    BaseApp.APP.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

  companion object {
    private const val CLIP_LABEL = "CLIP_LABEL"
    private var instance: ClipboardUtil? = null
      get() {
        if (field == null) {
          field = ClipboardUtil()
        }
        return field
      }

    //细心的小伙伴肯定发现了，这里不用getInstance作为为方法名，是因为在伴生对象声明时，内部已有getInstance方法，所以只能取其他名字
    fun get(): ClipboardUtil {

      return instance!!
    }
  }

  /**
   * 将数据拷贝到剪切板中，并在30s后清空剪切板
   */
  public fun copyDataToClip(string: String) {
    val itemData = ClipData.newPlainText(CLIP_LABEL, string)
    clipManager.setPrimaryClip(itemData)
    val time = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getString(
            BaseApp.APP.getString(
                R.string.set_key_clean_clip_time
            ), "30"
        )!!.toLong()
    val wordRequest = OneTimeWorkRequest.Builder(CleanClipWord::class.java)
        .setInitialDelay(time, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(BaseApp.APP)
        .enqueue(wordRequest)
  }

  /**
   * 清空剪切板
   */
  fun cleanClipboard() {
    clipManager.setPrimaryClip(ClipData.newPlainText(null, ""))
  }

  class CleanClipWord(
    appContext: Context,
    workerParams: WorkerParameters
  ) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
      get().cleanClipboard()
      return Result.success()
    }

  }

}