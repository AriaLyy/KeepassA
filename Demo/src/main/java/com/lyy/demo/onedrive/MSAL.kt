/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo.onedrive

import android.util.Log
import com.arialyy.frame.base.net.INetResponse
import com.arialyy.frame.base.net.NetManager1
import com.arialyy.frame.util.FileUtil
import com.microsoft.identity.client.IAuthenticationResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author laoyuyu
 * @date 2021/2/6
 */
object MSAL {
  private val TAG = "MSAL"
  private val netManager: NetManager1
  private val pool = ThreadPoolExecutor(
      16,
      60,
      20,
      SECONDS,
      LinkedBlockingQueue(),
      ThreadFactory {
        android.os.Process.setThreadPriority(
            android.os.Process.THREAD_PRIORITY_BACKGROUND + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE
        )
        return@ThreadFactory Thread(it, "MSAL-Thread")
      }
  ).asCoroutineDispatcher()
  private var authInfo: IAuthenticationResult? = null

  /**
   * 应用更目录
   */
  const val APP_ROOT_DIR = "approot"

  /**
   * token key
   */
  const val TOKEN_KEY = "Authorization"

  /**
   * 成功
   */
  const val STATE_SUCCESS = 1

  /**
   * 失败
   */
  const val STATE_FAIL = 0

  private const val TIME_OUT = 1L
  private const val BASE_URL = "https://graph.microsoft.com/v1.0/"

  init {
    netManager = NetManager1().builderManager(BASE_URL, arrayListOf())
  }

  fun updateAuthInfo(authInfo: IAuthenticationResult?) {
    this.authInfo = authInfo
  }

  private fun getAuthInfo() = authInfo!!

  fun updateNewFile(
    userId: String,
    itemSource: MsalSourceItem,
    response: INetResponse<MsalSourceItem>
  ) {
    val handler = CoroutineExceptionHandler { _, exception ->
      if (exception is NoDataError || exception is RequestFailError) {
        Log.e(TAG, exception.message ?: "")
      }
      response.onFailure(exception)
    }
    GlobalScope.launch(pool + handler) {
      val data = netManager.request(MsalApi::class.java)
          .updateFile(getAuthInfo().accessToken, userId, itemSource.id)
      handlerResponse(data, response)
    }
  }

  /**
   * 上传新文件
   */
  fun uploadNewFile(
    userId: String,
    filePath: String,
    response: INetResponse<MsalSourceItem>
  ) {
    val handler = CoroutineExceptionHandler { _, exception ->
      if (exception is NoDataError || exception is RequestFailError) {
        Log.e(TAG, exception.message ?: "")
      }
      response.onFailure(exception)
    }
    GlobalScope.launch(pool + handler) {
      val data = netManager.request(MsalApi::class.java)
          .uploadNewFile(getAuthInfo().accessToken, userId, APP_ROOT_DIR, filePath)
      handlerResponse(data, response)
    }
  }

  /**
   * 下载文件
   * @return [STATE_SUCCESS] 成功，[STATE_FAIL] 失败
   */
  suspend fun download(
    userId: String,
    itemSource: MsalSourceItem,
    filePath: String
  ): Int {
    return withContext(pool) {
      val hb = Headers.Builder()
          .add(TOKEN_KEY, getAuthInfo().accessToken)
          .build()
      val request: Request = Request.Builder()
          .url("$BASE_URL/users/${userId}/drive/items/${itemSource.id}/content")
          .headers(hb)
          .build()
      val call = netManager.getClient()
          .newCall(request)

      try {
        val response = call.execute()
        if (!response.isSuccessful) return@withContext STATE_FAIL
        val byteSystem = response.body?.byteStream() ?: return@withContext STATE_FAIL
        val outF = File(filePath)
        val fr = FileUtil.createFile(outF)
        if (!fr) {
          Log.e(TAG, "创建文件失败，path = $filePath")
          return@withContext STATE_FAIL
        }
        var len = 0
        val buf = ByteArray(1024)
        val fos = FileOutputStream(outF)
        do {
          len = byteSystem.read(buf)
          if (len != -1) {
            fos.write(buf, 0, len)
          }
        } while (len != -1)
      } catch (e: Exception) {
        e.printStackTrace()
        return@withContext STATE_FAIL
      }

      return@withContext STATE_SUCCESS
    }
  }

  /**
   * 获取驱动器列表
   */
  fun getAppDir(
    userId: String,
    response: INetResponse<List<MsalSourceItem>>,
  ) {
    val handler = CoroutineExceptionHandler { _, exception ->
      if (exception is NoDataError || exception is RequestFailError) {
        Log.e(TAG, exception.message ?: "")
      }
      response.onFailure(exception)
    }
    GlobalScope.launch(pool + handler) {
      val data = netManager.request(MsalApi::class.java)
          .getAppFolder(getAuthInfo().accessToken, userId, APP_ROOT_DIR)
      handlerResponse(data, response)
    }
  }

  /**
   * 获取驱动器列表
   */
  fun getDriveList(
    userId: String,
    response: INetResponse<List<DriveItem>>,
  ) {
    val handler = CoroutineExceptionHandler { _, exception ->
      if (exception is NoDataError || exception is RequestFailError) {
        Log.e(TAG, exception.message ?: "")
      }
      response.onFailure(exception)
    }
    GlobalScope.launch(pool + handler) {
      val data = netManager.request(MsalApi::class.java)
          .getDriveList(getAuthInfo().accessToken, userId)
      handlerResponse(data, response)
    }
  }

  private fun <T> handlerResponse(
    data: MsalResponse<T>?,
    response: INetResponse<T>
  ) {
    if (data?.value == null) {
      response.onFailure(NoDataError())
      return
    }
    if (data.error != null) {
      response.onFailure(RequestFailError(data.error))
      return
    }
    response.onResponse(data.value)
  }

}

/**
 * 请求异常
 */
class RequestFailError(msg: MsalErrorInfo) : Exception(msg.toString())

/**
 * 没有数据的异常
 */
class NoDataError : NullPointerException("返回数据为空")

interface MSALError
