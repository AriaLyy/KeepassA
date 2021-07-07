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
import androidx.core.net.toFile
import com.arialyy.frame.util.FileUtil
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.entity.DbHistoryRecord
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import timber.log.Timber
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.util.Date

/**
 * webdav工具
 */
object WebDavUtil : ICloudUtil {
  private val TAG = StringUtil.getClassName(this)
  var sardine: OkHttpSardine? = null
  var fileName: String = ""

  /**
   * 是否登录
   * @return false 未登录
   */
  fun isLogin(): Boolean {
    return sardine != null
  }

  fun createDir(path: String) {
    sardine?.createDirectory(path)
  }

  /**
   * 检查登录
   * @return true 登录成功，false 登录失败
   */
  fun checkLogin(
    userName: String,
    password: String
  ): Boolean {
    try {
      sardine = OkHttpSardine()
      sardine?.setCredentials(userName, password)
    } catch (e: Exception) {
      sardine = null
      e.printStackTrace()
      Timber.e(e, "checkLogin")
      return false
    }
    return true
  }

  /**
   * 进行登录
   * 创建一个小文件上传成成功后并删除
   */
  fun login(
    uri: String,
    userName: String,
    password: String
  ): OkHttpSardine {
    sardine = OkHttpSardine()
    sardine?.let {
      it.setCredentials(userName, password)
    }
    return sardine as OkHttpSardine
  }

  override fun getRootPath(): String {
    return "/"
  }

  override suspend fun getFileList(cloudPath: String): List<CloudFileInfo>? {
    sardine ?: return null
    val list = ArrayList<CloudFileInfo>()
    try {
      val resources = sardine!!.list(convertUrl(cloudPath))
      if (resources == null || resources.isEmpty()) {
        return null
      }

      for (file in resources) {
        list.add(
          CloudFileInfo(file.path, file.name, file.modified, file.contentLength, file.isDirectory)
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e,"获取文件列表失败")
    }
    return list
  }

  /**
   * 不支持比对
   */
  override suspend fun checkContentHash(
    cloudFileHash: String,
    localFileUri: Uri
  ): Boolean {
    return false
  }

  override suspend fun getFileInfo(fileKey: String): CloudFileInfo? {
    Timber.i("获取文件信息，cloudPath：$fileKey")
    try {
      sardine ?: return null
      val resources = sardine!!.list(convertUrl(fileKey))
      if (resources == null || resources.isEmpty()) {
        return null
      }
      val file = resources[0]
      return CloudFileInfo(
        file.path, file.name, file.modified, file.contentLength, file.isDirectory
      )
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e,"获取文件信息失败")
    }
    return null
  }

  override suspend fun delFile(fileKey: String): Boolean {
    sardine ?: return false
    try {
      sardine!!.delete(convertUrl(fileKey))
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e,"删除文件失败")
      return false
    }
    return true
  }

  override suspend fun getFileServiceModifyTime(fileKey: String): Date {
    sardine ?: return Date()
    val cloudInfo = getFileInfo(convertUrl(fileKey))
    cloudInfo ?: return Date()
    return cloudInfo.serviceModifyDate
  }

  override suspend fun uploadFile(
    context: Context,
    dbRecord: DbHistoryRecord
  ): Boolean {
    sardine ?: return false
    try {
      sardine!!.put(
        dbRecord.cloudDiskPath, Uri.parse(dbRecord.localDbUri)
          .toFile(), "*/*"
      )
      val info = getFileInfo(dbRecord.cloudDiskPath!!)
      if (info != null) {
        DbSynUtil.serviceModifyTime = info.serviceModifyDate
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e,"上传文件失败")
    }

    return true
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String? {
    sardine ?: return null
    Timber.d("start download file, save path: $filePath")
    val cloudPath = convertUrl(dbRecord.cloudDiskPath.toString())
    val fp = filePath.toFile()
    if (!fp.exists()) {
      FileUtil.createFile(fp)
    }
    sardine?.let {
      var token = ""
      try {
        token = it.lock(cloudPath)
        val ips = it.get(cloudPath)
        val fileInfo = getFileInfo(cloudPath)
        val fic = Channels.newChannel(ips)
        val foc = FileOutputStream(fp).channel
        foc.transferFrom(fic, 0, fileInfo!!.size)
      } catch (e: Exception) {
        e.printStackTrace()
        Timber.e(e,"下载文件失败")
        return null
      } finally {
        it.unlock(cloudPath, token)
      }
    }

    return filePath.toString()
  }

  /**
   * 将含有中文的url进行格式化
   */
  private fun convertUrl(url: String): String {
    return url
  }
}