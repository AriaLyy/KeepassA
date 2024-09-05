/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud.google

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveId
import com.google.android.gms.drive.OpenFileActivityOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.lyy.keepassa.util.cloud.GoogleDriveUtil
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2024/5/29
 **/
class GoogleSelectFileActivity : AppCompatActivity() {
  private val REQUEST_CODE_OPEN_FILE = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    selectFile()
  }

  private fun selectFile() {
    val openFileActivityOptions = OpenFileActivityOptions.Builder()
      .setMimeType(
        mutableListOf(
          "image/jpeg"
        )
      )
      .build()
    val intentSenderTask =
      GoogleDriveUtil.getDriveClient(this)?.newOpenFileActivityIntentSender(openFileActivityOptions)
    intentSenderTask?.continueWithTask<Any> { task: Task<IntentSender?> ->
      try {
        val intentSender = task.result
        startIntentSenderForResult(
          intentSender!!,
          REQUEST_CODE_OPEN_FILE,
          null,
          0,
          0,
          0,
          null
        )
        return@continueWithTask null
      } catch (e: Exception) {
        Timber.e(e,"Unable to create file picker intent")
        return@continueWithTask Tasks.forException<Any>(e)
      }
    }?.addOnFailureListener { e -> Timber.e(e,"Failed to start file picker activity") }


  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK) {
      val driveId =
        data?.getParcelableExtra<DriveId>(OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID)
      if (driveId == null) {
        Timber.w("driveId is null")
        return
      }
      // 现在你可以使用driveId来访问或操作用户选择的文件
      retrieveFileContent(driveId)
    } else {
      Timber.e("File selection cancelled or failed.")
    }
  }

  // 示例方法：检索并显示文件内容
  private fun retrieveFileContent(driveId: DriveId) {
    val resourceClient = GoogleDriveUtil.getDriveResourceClient(this)

    resourceClient?.openFile(driveId.asDriveFile(), DriveFile.MODE_READ_ONLY)
      ?.addOnSuccessListener { driveContents ->
        // 读取文件内容，这里以读取文本内容为例
        var content: String? = null
        // try {
        //   content = IOUtils.toString(driveContents.inputStream)
        //   Log.i(TAG, "File content: $content")
        // } catch (e: IOException) {
        //   Log.e(TAG, "Error reading file contents", e)
        // } finally {
        //   // 关闭DriveContents以释放资源
        //   driveContents.close()
        // }
      }
      ?.addOnFailureListener { e -> Timber.e("Unable to open file", e) }
  }
}