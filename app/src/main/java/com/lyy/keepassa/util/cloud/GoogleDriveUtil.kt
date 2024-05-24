/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.EncryptUtils
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.KpaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections
import java.util.Date

object GoogleDriveUtil : ICloudUtil {
  internal val GOOGLE_AUTH_FLOW = MutableSharedFlow<HttpRequestInitializer>(0)
  val AUTH_STATE_FLOW = MutableSharedFlow<Boolean>(0)

  private var service: Drive? = null
  private const val APP_SPACES = "appDataFolder"
  private const val DRIVE_SPACES = "drive"

  init {
    KpaUtil.scope.launch {
      GOOGLE_AUTH_FLOW.collectLatest {
        service = Drive.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory(),
          it
        )
          .setApplicationName("KeepassA")
          .build()
        AUTH_STATE_FLOW.emit(true)
      }
    }
  }

  override fun auth() {
    ActivityUtils.getTopActivity().apply {
      startActivity(Intent(this, GoogleAuthActivity::class.java))
    }
  }

  override suspend fun fileExists(fileKey: String): Boolean {
    return getFileInfo(fileKey) != null
  }

  override fun getRootPath(): String {
    return service?.servicePath ?: "/"
  }

  override suspend fun getFileList(dirPath: String): List<CloudFileInfo>? {
    return withContext(Dispatchers.IO) {
      val fList = service?.files()?.list()?.setSpaces(DRIVE_SPACES)?.execute()
      fList ?: return@withContext null
      val list = arrayListOf<CloudFileInfo>()
      fList.files.forEach {
        list.add(it.toCloudFileInfo()!!)
      }
      if (list.isEmpty()) {
        // init appdata folder
        createInitFile()
      }
      return@withContext list
    }
  }

  override suspend fun checkContentHash(cloudFileHash: String?, localFileUri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
      BaseApp.APP.contentResolver.openInputStream(localFileUri).use {
        EncryptUtils.encryptMD5(it?.readBytes())
      }

      return@withContext false
    }
  }

  override suspend fun getFileInfo(fileKey: String): CloudFileInfo? {
    return withContext(Dispatchers.IO) {
      val meta = service?.files()?.get(fileKey)?.execute() ?: return@withContext null
      val info = meta.toCloudFileInfo()
      Timber.d(info.toString())
      return@withContext info
    }
  }

  override suspend fun delFile(fileKey: String): Boolean {
    kotlin.runCatching {
      withContext(Dispatchers.IO) {
        service?.files()?.delete(fileKey)?.execute()
      }
      return true
    }.onFailure {
      return false
    }.onSuccess {
      return true
    }.getOrNull() ?: return false
  }

  override suspend fun getFileServiceModifyTime(fileKey: String): Date {
    return getFileInfo(fileKey)?.serviceModifyDate ?: Date(System.currentTimeMillis())
  }

  override suspend fun uploadFile(context: Context, dbRecord: DbHistoryRecord): Boolean {
    val cloudFileInfo =

      withContext(Dispatchers.IO) {
        val cloudFileInfo = getFileInfo(dbRecord.cloudDiskPath ?: "")
        if (cloudFileInfo == null) {
          // createFile()
        }
      }

    return false
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String? {
    TODO("Not yet implemented")
  }

  private fun createInitFile() {


    val tempFileName = "KeepassA Folder"
    val tempFile = java.io.File("${BaseApp.APP.cacheDir.path}/${tempFileName}")
    if (!tempFile.exists()) {
      tempFile.createNewFile()
    }
    val fileMetadata = File()
    fileMetadata.setName(tempFileName)
    fileMetadata.setParents(Collections.singletonList(DRIVE_SPACES))
    service?.files()?.create(fileMetadata, FileContent("text/plain", tempFile))?.execute()
  }
}

/**
 * https://developers.google.com/drive/api/reference/rest/v3/files?hl=zh-cn
 */
fun File?.toCloudFileInfo(): CloudFileInfo? {
  this?.labelInfo?.hashCode()
  if (this == null) return null
  return CloudFileInfo(
    id,
    name,
    Date(modifiedTime?.value ?: System.currentTimeMillis()),
    size.toLong(),
    kind != "drive#file",
    md5Checksum
  )
}