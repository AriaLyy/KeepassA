/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.cloud

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.arialyy.frame.util.SharePreUtil
import com.arialyy.frame.util.StringUtil
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DeletedMetadata
import com.dropbox.core.v2.files.FileMetadata
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.QuickUnLockUtil
import java.util.Date

/**
 * dropbox 工具
 */
object DropboxUtil : ICloudUtil {
  private val TAG = StringUtil.getClassName(this)
  val APP_KEY = "ib45r6jnfz3oakq"
  private val DROPBOX_KEY_TOKEN = "DROPBOX_KEY_TOKEN"

  /**
   * 是否申请短期令牌
   * {@code true} 使用短期令牌
   */
  private val USE_SLT = false

  private var sDbxClient: DbxClientV2? = null

  private var sDbxRequestConfig: DbxRequestConfig? = null

  /**
   * 配置dropbox请求信息
   */
  private fun getRequestConfig(): DbxRequestConfig? {
    if (sDbxRequestConfig == null) {
      sDbxRequestConfig = DbxRequestConfig.newBuilder("keepassA")
          .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
          .build()
    }
    return sDbxRequestConfig
  }

  /**
   * 通过token 初始化客户端
   */
  @Synchronized
  private fun init(accessToken: String) {
    if (sDbxClient == null) {
      sDbxClient = DbxClientV2(
          getRequestConfig(), accessToken
      )
    }
  }

  /**
   * 通过令牌初始化客户端
   */
  @Synchronized
  private fun init(credential: DbxCredential) {
    val temp = DbxCredential(
        credential.accessToken, -1L, credential.refreshToken, credential.appKey
    )
    if (sDbxClient == null) {
      sDbxClient = DbxClientV2(
          getRequestConfig(), temp
      )
    }
  }

  /**
   * 获取dropbox 客户端
   * @retun 如果没有授权，返null，需要进入授权页面[Auth.startOAuth2Authentication]
   */
  fun getClient(): DbxClientV2? {
    var token = getLocalToken()
    if (TextUtils.isEmpty(token)) {
      token = Auth.getOAuth2Token()
      if (!TextUtils.isEmpty(token)) {
        saveToken(token)
      } else {
        return null
      }
    }
    if (token != null) {
      init(token)
    }
    return sDbxClient
  }

  /**
   * https://github.com/dropbox/dropbox-sdk-obj-c/issues/32
   * dropbox 的根路径是：""
   */
  override fun getRootPath(): String {
    return ""
  }

  override suspend fun getFileList(path: String): List<CloudFileInfo>? {
    Log.d(TAG, "开始获取文件列表，path = $path")
    val client = getClient() ?: return null
    val entries = client.files()
        .listFolder(path).entries
    if (entries.isEmpty()) {
      return null
    }

    val list = ArrayList<CloudFileInfo>()
    val root = if (TextUtils.isEmpty(path)) "" else path
    for (e in entries) {
      if (e is FileMetadata) {
        list.add(
            CloudFileInfo("$root/${e.name}", e.name, e.serverModified, e.size, false, e.contentHash)
        )
      } else {
        list.add(CloudFileInfo("$root/${e.name}", e.name, Date(), 0, true))
      }
    }
    return list
  }

  /**
   * [hash 算法](https://www.dropbox.com/developers/reference/content-hash)
   *
   */
  override suspend fun checkContentHash(
    cloudFileHash: String,
    localFileUri: Uri
  ): Boolean {
    val hasher = DropboxContentHasher()
    val buf = ByteArray(1024)
    val ips = UriUtil.getUriInputStream(BaseApp.APP, localFileUri)
    while (true) {
      val n = ips.read(buf)
      if (n < 0) break
      hasher.update(buf, 0, n)
    }
    val localHash = DropboxContentHasher.hex(hasher.digest())
    ips.close()
    Log.i(TAG, "本地文件hash: $localHash")
    return cloudFileHash.equals(localHash, ignoreCase = true)
  }

  override suspend fun getFileInfo(fileKey: String): CloudFileInfo? {
    val client = getClient() ?: return null
    try {
      val entries = client.files()
          .listRevisions(fileKey).entries
      val entry = entries[0]
      return CloudFileInfo(
          fileKey, entry.name, entry.serverModified, entry.size, true, entry.contentHash
      )
    } catch (e: Exception) {
      e.printStackTrace()

    }
    return null
  }

  override suspend fun delFile(fileKey: String): Boolean {
    Log.d(TAG, "删除云端文件: $fileKey")
    val d = getClient()
        ?.files()
        ?.deleteV2(fileKey)
    if (d == null || d.metadata == null || d.metadata is DeletedMetadata) {
      return false
    }
    return true
  }

  /**
   * 获取服务器端文件的修改时间
   */
  override suspend fun getFileServiceModifyTime(fileKey: String): Date {
    val client = getClient() ?: return Date(System.currentTimeMillis())
    val entries = client.files()
        .listRevisions(fileKey).entries
    return entries[0].serverModified
  }

  /**
   * 上传文件
   */
  override suspend fun uploadFile(
    context: Context,
    dbRecord: DbHistoryRecord
  ): Boolean {

    val dbUri = Uri.parse(dbRecord.localDbUri)
    val cloudDiskPath = dbRecord.cloudDiskPath

    val ips = BaseApp.APP.contentResolver.openInputStream(dbUri)
    val fd = getClient()
        ?.files()
        ?.uploadBuilder(cloudDiskPath)
        ?.uploadAndFinish(ips)
    if (fd != null) {
      DbSynUtil.serviceModifyTime = fd.serverModified
    }
    ips?.close()
    return true
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String? {
    val client = getClient() ?: return null
    val os = context.contentResolver.openOutputStream(filePath)
    client.files()
        .download(dbRecord.cloudDiskPath)
        .download(os)
    os?.let {
      it.flush()
      it.close()
    }
    return filePath.toString()
  }

  /**
   * 保存token
   */
  fun saveToken(token: String) {
    SharePreUtil.putString(
        Constance.PRE_FILE_NAME, BaseApp.APP,
        DROPBOX_KEY_TOKEN,
        QuickUnLockUtil.encryptStr(token)
    )
  }

  /**
   * 获取本地保存的token
   * @return 没有保存的token，返回null
   */
  private fun getLocalToken(): String? {
    val token = SharePreUtil.getString(
        Constance.PRE_FILE_NAME, BaseApp.APP,
        DROPBOX_KEY_TOKEN
    )
    return if (!TextUtils.isEmpty(token)) {
      QuickUnLockUtil.decryption(token)
    } else {
      null
    }
  }

  /**
   * 判断是否登录成功，如果没有授权，请使用[Auth.startOAuth2Authentication]启动授权界面
   * @return false 没有登录
   */
  fun isAuthorized(): Boolean {
    val token = SharePreUtil.getString(
        Constance.PRE_FILE_NAME, BaseApp.APP,
        DROPBOX_KEY_TOKEN
    )
    return !TextUtils.isEmpty(token)
  }
}