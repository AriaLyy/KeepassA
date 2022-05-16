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
import com.lyy.keepassa.entity.DbHistoryRecord
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import timber.log.Timber
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.util.Date

/**
 * webdav工具
 */
object WebDavUtil : ICloudUtil {
  val SUPPORTED_WEBDAV_URLS = mutableListOf<String>().apply {
    add("https://dav.jianguoyun.com")
    add("https://dav.box.com")
    add("https://webdav.4shared.com")
    // add("https://my.powerfolder.com/webdav") // 只能用他家的client登录？电脑qspase可以
    // add("https://dav.dropdav.com") // 需要注册：https://app.dropdav.com/users/sign_in
    // add("https://webdav.yandex.com") 需要使用sdk, htts://yandex.com/dev/id/
    add("other")
  }

  val REMOVE_PARENT_URLS = mutableListOf<String>().apply {
    add("https://dav.jianguoyun.com")
    add("https://dav.box.com")
    add("https://webdav.4shared.com")
  }

  var sardine: OkHttpSardine? = null
  var fileName: String = ""
  private var hostUri: String = ""
  var userName: String = ""
  var password: String = ""

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

  fun setHostUri(hostUri: String) {
    this.hostUri = if (hostUri.endsWith("/")) hostUri.substring(0, hostUri.length - 1) else hostUri
  }

  fun getHostUri() = hostUri

  /**
   * 检查登录
   * @return true 登录成功，false 登录失败
   */
  fun checkLogin(
    uri: String,
    userName: String,
    password: String
  ): Boolean {
    this.userName = userName
    this.password = password
    setHostUri(uri)
    sardine = OkHttpSardine()
    sardine?.setCredentials(userName, password, true)
    try {
      val list = sardine?.list(uri)
      return !list.isNullOrEmpty()
    } catch (e: Exception) {
      Timber.e(e)
    }
    return false
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
    this.userName = userName
    this.password = password
    setHostUri(uri)
    sardine = OkHttpSardine()
    sardine?.setCredentials(userName, password, true)
    return sardine as OkHttpSardine
  }

  override suspend fun fileExists(fileKey: String): Boolean {
    sardine ?: return false
    Timber.d("fileExists, fileKey = $fileKey")
    try {
      return sardine!!.exists(fileKey)
    } catch (e: Exception) {
      Timber.e(e)
    }
    return false
  }

  override fun getRootPath(): String {
    return "/"
  }

  /**
   *@param dirPath 相对路径，如：/dav/
   */
  override suspend fun getFileList(dirPath: String): List<CloudFileInfo>? {
    sardine ?: return null
    Timber.d("getFileList, host = ${hostUri}, fileKey = $dirPath")
    val list = ArrayList<CloudFileInfo>()
    try {
      val resources = sardine!!.list(convertUrl("${hostUri}${dirPath}"))
      if (resources == null || resources.isEmpty()) {
        return null
      }

      for (file in resources) {
        list.add(
          CloudFileInfo(file.path, file.name, file.modified, file.contentLength, file.isDirectory)
        )
      }
      if (hostUri in REMOVE_PARENT_URLS) {
        // 坚果云移除第一个item
        list.removeAt(0)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e, "获取文件列表失败")
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
      Timber.e(e, "获取文件信息失败")
    }
    return null
  }

  override suspend fun delFile(fileKey: String): Boolean {
    sardine ?: return false
    try {
      sardine!!.delete(convertUrl(fileKey))
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e, "删除文件失败")
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
    Timber.d("uploadFile, cloudPath = ${dbRecord.cloudDiskPath}, localPath = ${dbRecord.localDbUri}")
    sardine ?: return false
    var localToken: String? = null
    try {
      // delFile(dbRecord.cloudDiskPath!!)
      // val exist = fileExists(dbRecord.cloudDiskPath!!)
      localToken = sardine!!.lock(dbRecord.cloudDiskPath, 5)
      Timber.d("localToken = $localToken")
      sardine?.put(
        dbRecord.cloudDiskPath,
        Uri.parse(dbRecord.localDbUri).toFile(),
        "application/binary",
        false,
        localToken
      )
      Timber.d("上传完成，重新获取文件信息")
      val info = getFileInfo(dbRecord.cloudDiskPath!!)
      if (info != null) {
        DbSynUtil.serviceModifyTime = info.serviceModifyDate
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Timber.e(e, "上传文件失败")
      return false
    } finally {
      localToken?.let {
        sardine?.unlock(dbRecord.cloudDiskPath!!, it)
      }
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
      var fic: ReadableByteChannel? = null
      var foc: FileChannel? = null
      try {
        token = it.lock(cloudPath)
        val ips = it.get(cloudPath)
        val fileInfo = getFileInfo(cloudPath)
        fic = Channels.newChannel(ips)
        foc = FileOutputStream(fp).channel
        foc.transferFrom(fic, 0, fileInfo!!.size)
      } catch (e: Exception) {
        e.printStackTrace()
        Timber.e(e, "下载文件失败")
        return null
      } finally {
        try {
          fic?.close()
          foc?.close()
        } catch (e: Exception) {
          Timber.e(e)
        }
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

  private fun getRelativePath() {
  }
}