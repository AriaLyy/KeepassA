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
import android.util.Log
import androidx.core.net.toFile
import com.arialyy.frame.util.FileUtil
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.util.KLog
import com.tencent.bugly.crashreport.BuglyLog
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.util.Date
import java.util.UUID

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

  /**
   * 检查登录，检查方案：
   * 1、创建一个临时文件
   * 2、上传该文件到webdav，如果上传成功表示登录成功
   * 3、删除云端和本地的临时文件
   * @return true 登录成功，false 登录失败
   */
  fun checkLogin(
    uri: String,
    userName: String,
    password: String
  ): Boolean {
    try {
      sardine = OkHttpSardine()
      sardine?.let {
        it.setCredentials(userName, password)
        // 1、创建临时文件
        it.put(uri, "${UUID.randomUUID()}".toByteArray(Charsets.UTF_8))
        // 2、删除云端文件
        it.delete(uri)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      BuglyLog.e(TAG, "checkLogin", e)
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
  ): OkHttpSardine? {
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
      BuglyLog.e(TAG, "获取文件列表失败", e)
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

  override suspend fun getFileInfo(cloudPath: String): CloudFileInfo? {
    Log.i(TAG, "获取文件信息，cloudPath：$cloudPath")
    try {
      sardine ?: return null
      val resources = sardine!!.list(convertUrl(cloudPath))
      if (resources == null || resources.isEmpty()) {
        return null
      }
      val file = resources[0]
      return CloudFileInfo(
          file.path, file.name, file.modified, file.contentLength, file.isDirectory
      )
    } catch (e: Exception) {
      e.printStackTrace()
      BuglyLog.e(TAG, "获取文件信息失败", e)
      e.printStackTrace()
    }
    return null
  }

  override suspend fun delFile(cloudPath: String): Boolean {
    sardine ?: return false
    try {
      sardine!!.delete(convertUrl(cloudPath))
    } catch (e: Exception) {
      e.printStackTrace()
      BuglyLog.e(TAG, "删除文件失败", e)
      return false
    }
    return true
  }

  override suspend fun getFileServiceModifyTime(cloudPath: String): Date {
    sardine ?: return Date()
    val cloudInfo = getFileInfo(convertUrl(cloudPath))
    cloudInfo ?: return Date()
    return cloudInfo.serviceModifyDate
  }

  override suspend fun uploadFile(
    context: Context,
    dbRecord: DbRecord
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
      BuglyLog.e(TAG, "上传文件失败", e)
    }

    return true
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbRecord,
    filePath: Uri
  ): String? {
    sardine ?: return null
    KLog.d(TAG, "start download file, save path: $filePath")
    val cloudPath = convertUrl(dbRecord.cloudDiskPath.toString())
    val fp = filePath.toFile()
    if (!fp.exists()){
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
        it.unlock(cloudPath, token)
      } catch (e: Exception) {
        e.printStackTrace()
        BuglyLog.e(TAG, "下载文件失败", e)
        return null
      }finally {
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