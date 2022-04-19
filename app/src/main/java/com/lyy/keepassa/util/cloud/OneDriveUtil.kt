/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.arialyy.frame.base.net.NetManager1
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.util.FileUtil
import com.google.gson.Gson
import com.lyy.keepassa.BuildConfig
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.ondrive.MsalApi
import com.lyy.keepassa.ondrive.MsalSourceItem
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KeepassAUtil
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.SignOutCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.util.Date

/**
 * Onedrive util
 */
object OneDriveUtil : ICloudUtil {

  /**
   * 应用根目录
   */
  const val APP_ROOT_DIR = "approot"

  /**
   * token key
   */
  const val TOKEN_KEY = "Authorization"

  private const val BASE_URL = "https://graph.microsoft.com/v1.0/"

  private lateinit var oneDriveApp: ISingleAccountPublicClientApplication
  private var authInfo: IAuthenticationResult? = null
  private val netManager by lazy {
    NetManager1().builderManager(BASE_URL, arrayListOf())
  }
  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  var loginCallback: OnLoginCallback? = null
  private var getTokenFailNum = 0
  private val MAX_FAIL_NUM = 1
  private val okClient by lazy {
    OkHttpClient()
  }

  fun isInitialized(): Boolean = this::oneDriveApp.isInitialized

  interface OnLoginCallback {
    fun callback(success: Boolean)
  }

  private fun getContext(): Context {
    return BaseApp.APP
  }

  private fun getCurActivity(): Activity {
    return AbsFrame.getInstance().currentActivity!!
  }

  private fun getAuthInfo() = authInfo!!

  private fun getUserId() = authInfo?.account?.id ?: ""

  /**
   * check login
   * @return true login success
   */
  private fun checkLogin(): Boolean {
    if (authInfo == null || !this::oneDriveApp.isInitialized) {
      Timber.e("登陆失败，sdk没有初始化，或登陆失败")
      HitUtil.toaskShort("${getContext().getString(R.string.login)}${getContext().getString(R.string.fail)}")
      return false
    }
    return true
  }

  /**
   * 初始化
   */
  fun initOneDrive(callback: (Boolean) -> Unit) {
    PublicClientApplication.createSingleAccountPublicClientApplication(
      getContext(),
      if (BuildConfig.DEBUG) R.raw.auth_config_single_account_debug else R.raw.auth_config_single_account_release,
      object : ISingleAccountApplicationCreatedListener {
        override fun onCreated(application: ISingleAccountPublicClientApplication) {
          /**
           * This test app assumes that the app is only going to support one account.
           * This requires "account_mode" : "SINGLE" in the config json file.
           */
          oneDriveApp = application
          Timber.d("初始化成功")
          callback.invoke(true)
        }

        override fun onError(exception: MsalException) {
          exception.printStackTrace()
          callback.invoke(false)
        }
      })
  }

  /**
   * 加载用户，没有登陆过，则需要重新登陆
   */
  fun loadAccount() {
    if (!this::oneDriveApp.isInitialized) {
      Timber.e("还没有初始化sdk")
      return
    }

    oneDriveApp.getCurrentAccountAsync(object : CurrentAccountCallback {
      override fun onAccountLoaded(activeAccount: IAccount?) {
        if (activeAccount == null) {
          Timber.w("用户还没有登陆")
          login()
          return
        }
        Timber.w("已经登陆过，自动登陆，开始获取token")
        getTokenByAccountInfo(activeAccount)
      }

      override fun onAccountChanged(
        priorAccount: IAccount?,
        currentAccount: IAccount?
      ) {
        Timber.w("账号已却换，重新获取token")
        if (currentAccount == null) {
          Timber.e("当前账户为空")
          return
        }
        getTokenByAccountInfo(currentAccount)
      }

      override fun onError(exception: MsalException) {
        exception.printStackTrace()
      }
    })
  }

  /**
   * 根据用户信息获取token
   */
  private fun getTokenByAccountInfo(account: IAccount) {
    oneDriveApp.acquireTokenSilentAsync(getScopes(), account.authority, object :
      SilentAuthenticationCallback {
      override fun onSuccess(authenticationResult: IAuthenticationResult?) {
        Timber.d("获取token成功")
        authInfo = authenticationResult
        loginCallback?.callback(true)
      }

      override fun onError(exception: MsalException) {
        Timber.d("获取token失败，重新启动登陆流程")
        exception.printStackTrace()
        if (getTokenFailNum >= MAX_FAIL_NUM) {
          HitUtil.toaskShort(R.string.get_token_fail)
          loginCallback?.callback(false)
          return
        }
        oneDriveApp.signOut(object : SignOutCallback {
          override fun onSignOut() {
            Timber.d("登出成功，重新开始登陆流程")
            login()
            getTokenFailNum += 1
          }

          override fun onError(exception: MsalException) {
            Timber.e("登出失败")
            exception.printStackTrace()
            loginCallback?.callback(false)
          }
        })
      }
    })
  }

  private fun login() {
    oneDriveApp.signIn(getCurActivity(), "", getScopes(), object : AuthenticationCallback {
      override fun onSuccess(authenticationResult: IAuthenticationResult?) {
        authInfo = authenticationResult
        HitUtil.toaskShort("${getContext().getString(R.string.login)}${getContext().getString(R.string.success)}")
        Timber.d("登陆成功")
        loginCallback?.callback(true)
      }

      override fun onError(exception: MsalException?) {
        HitUtil.toaskShort("${getContext().getString(R.string.login)}${getContext().getString(R.string.fail)}")
        Timber.d("登陆失败")
        exception?.printStackTrace()
        loginCallback?.callback(false)
      }

      override fun onCancel() {
        Timber.d("取消登陆")
        HitUtil.toaskShort("${getContext().getString(R.string.login)}${getContext().getString(R.string.cancel)}")
        loginCallback?.callback(false)
      }
    })
  }

  private fun getScopes(): Array<String> {
    return arrayOf("User.Read", "Files.ReadWrite.AppFolder")
  }

  private fun msalItem2CloudItem(item: MsalSourceItem): CloudFileInfo {
    return CloudFileInfo(
      fileKey = item.id,
      fileName = item.name,
      serviceModifyDate = dateFormat.parseDateTime(item.lastModifiedDateTime)
        .toDate(),
      size = item.size,
      isDir = item.isFolder(),
      contentHash = if (item.isFolder()) null else item.file?.hashes?.sha256Hash
    )
  }

  override suspend fun fileExists(fileKey: String): Boolean {
    return getFileInfo(fileKey) != null
  }

  override fun getRootPath(): String {
    return "/"
  }

  override suspend fun getFileList(path: String): List<CloudFileInfo>? {
    if (!checkLogin()) {
      return null
    }
    Timber.d("获取文件列表，path = $path")
    val response = if (path == "/") {
      netManager.request(MsalApi::class.java)
        .getAppFolderList(getAuthInfo().accessToken, getUserId())
    } else {
      netManager.request(MsalApi::class.java)
        .getFolderListById(getAuthInfo().accessToken, getUserId(), path)
    }
    if (response.value == null) {
      return null
    }
    val fileList = arrayListOf<CloudFileInfo>()

    response.value.forEach {
      fileList.add(msalItem2CloudItem(it))
    }

    return fileList
  }

  override suspend fun checkContentHash(
    cloudFileHash: String,
    localFileUri: Uri
  ): Boolean {
    return false
  }

  override suspend fun getFileInfo(fileKey: String): CloudFileInfo? {
    val userId = getUserId()
    Timber.d("getFileInfo, userId = ${userId}, fileKey = $fileKey")

    try {
      val response: MsalSourceItem? = if (fileKey.startsWith("/")) {
        netManager.request(MsalApi::class.java)
          .getFileInfoByPath(
            getAuthInfo().accessToken,
            userId,
            fileKey.substring(1, fileKey.length)
          )
      } else {
        netManager.request(MsalApi::class.java)
          .getFileInfoById(getAuthInfo().accessToken, userId, fileKey)
      }
      if (response == null) {
        Timber.e("获取文件信息失败")
        return null
      }
      return msalItem2CloudItem(response)
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }

  /**
   * 如果成功，此调用将返回 204 No Content 响应，以指明资源已被删除，没有可返回的内容。
   */
  override suspend fun delFile(fileKey: String): Boolean {
    Timber.d("删除文件，fileKey = $fileKey")
    val response = netManager.request(MsalApi::class.java)
      .deleteFile(getAuthInfo().accessToken, getUserId(), fileKey)
    return response.code() == HttpURLConnection.HTTP_NO_CONTENT
  }

  override suspend fun getFileServiceModifyTime(fileKey: String): Date {
    val fileInfo = getFileInfo(fileKey) ?: return Date(System.currentTimeMillis())
    return fileInfo.serviceModifyDate
  }

  override suspend fun uploadFile(
    context: Context,
    dbRecord: DbHistoryRecord
  ): Boolean {
    val file = File(Uri.parse(dbRecord.localDbUri).path!!)
    try {
      // 创建上传session
      val uploadSession = netManager.request(MsalApi::class.java)
        .createUploadSession(
          authorization = getAuthInfo().accessToken,
          userId = getUserId(),
          itemPath = file.name
        )

      Timber.d("获取session成功，上传地址：${uploadSession.uploadUrl}")
      // 取消上传的临时文件会话
      cancelUploadSession(uploadSession.uploadUrl)

      // 开始上传文件
      val desc = file.asRequestBody("multipart/form-data".toMediaType())
      val body = MultipartBody.Part.createFormData("file", file.name, desc)
      val fileSize = file.length()
      val request = Request.Builder()
        .header("Content-Length", fileSize.toString())
        .header("Content-Range", "bytes 0-${fileSize - 1}/${fileSize}")
        .url(uploadSession.uploadUrl)
        .put(body = body.body)
        .build()
      val response = okClient.newCall(request)
        .execute()
      if (response.code != HttpURLConnection.HTTP_OK && response.code != HttpURLConnection.HTTP_CREATED) {
        Timber.e("上传失败，code = ${response.code}, msg = ${response.message}")
        return false
      }
      val responseBytes = response.body?.bytes()
      if (responseBytes == null) {
        Timber.e("body为null")
        return false
      }
      val responseContent = String(responseBytes, Charset.forName("UTF-8"))
      Timber.d("上传成功，响应内容")
      KLog.j("TAG", responseContent)
      val obj = Gson().fromJson(responseContent, MsalSourceItem::class.java)
      dbRecord.cloudDiskPath = obj.id
      KeepassAUtil.instance.saveLastOpenDbHistory(dbRecord)

      return true
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }

  /**
   * 取消上传会话
   */
  private fun cancelUploadSession(uploadUrl: String) {
    try {
      Timber.d("如果有的话，取消临时文件的上传对话")
      val request = Request.Builder()
        .url(uploadUrl)
        .delete()
        .build()
      val response = okClient.newCall(request)
        .execute()
      Timber.d("code = ${response.code}, body = ${response.body?.string()}")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String? {
    return withContext(Dispatchers.IO) {
      val hb = Headers.Builder()
        .add(TOKEN_KEY, getAuthInfo().accessToken)
        .build()
      val request: Request = Request.Builder()
        .url("$BASE_URL/users/${getUserId()}/drive/items/${dbRecord.cloudDiskPath}/content")
        .headers(hb)
        .build()
      val call = netManager.getClient()
        .newCall(request)

      try {
        val response = call.execute()
        if (!response.isSuccessful) {
          return@withContext null
        }
        val byteSystem = response.body?.byteStream() ?: return@withContext null
        val outF = File(filePath.path)
        val fr = FileUtil.createFile(outF)
        if (!fr) {
          Timber.e("创建文件失败，path = $filePath")
          return@withContext null
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
        return@withContext filePath.toString()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext null
    }
  }
}