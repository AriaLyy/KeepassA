package com.lyy.keepassa.util.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.entity.DbRecord
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
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
    sardine = OkHttpSardine()
    sardine?.let {
      it.setCredentials(userName, password)
      // 1、创建临时文件
      it.put(uri, "${UUID.randomUUID()}".toByteArray(Charsets.UTF_8))
      // 2、删除云端文件
      it.delete(uri)
    }
    return true
  }

  /**
   * 进行登录
   * 创建一个小文件上传成成功后并删除
   */
  suspend fun login(
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

  override suspend fun getFileList(path: String): List<CloudFileInfo>? {
    sardine ?: return null
    val resources = sardine!!.list(path)
    if (resources == null || resources.isEmpty()) {
      return null
    }
    val list = ArrayList<CloudFileInfo>()
    for (file in resources) {
      list.add(
          CloudFileInfo(file.path, file.name, file.modified, file.contentLength, file.isDirectory)
      )
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
      val resources = sardine!!.list(cloudPath)
      if (resources == null || resources.isEmpty()) {
        return null
      }
      val file = resources[0]
      return CloudFileInfo(
          file.path, file.name, file.modified, file.contentLength, file.isDirectory
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  override suspend fun delFile(cloudPath: String): Boolean {
    sardine ?: return false
    try {
      sardine!!.delete(cloudPath)
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
    return true
  }

  override suspend fun getFileServiceModifyTime(cloudPath: String): Date {
    sardine ?: return Date()
    val cloudInfo = getFileInfo(cloudPath)
    cloudInfo ?: return Date()
    return cloudInfo.serviceModifyDate
  }

  override suspend fun uploadFile(
    context: Context,
    dbRecord: DbRecord
  ): Boolean {
    sardine ?: return false
    sardine!!.put(
        dbRecord.cloudDiskPath, Uri.parse(dbRecord.localDbUri)
        .toFile(), "*/*"
    )
    val info = getFileInfo(dbRecord.cloudDiskPath!!)
    if (info != null) {
      DbSynUtil.serviceModifyTime = info.serviceModifyDate
    }
    return true
  }

  override suspend fun downloadFile(
    context: Context,
    dbRecord: DbRecord,
    filePath: Uri
  ): String? {
    sardine ?: return null
    val cloudPath = dbRecord.cloudDiskPath
    val token = sardine!!.lock(cloudPath)
    val ips = sardine!!.get(cloudPath)
    val fileInfo = getFileInfo(cloudPath!!)
    val fic = Channels.newChannel(ips)
    val foc = FileOutputStream(filePath.toFile()).channel
    foc.transferFrom(fic, 0, fileInfo!!.size)
    sardine!!.unlock(cloudPath, token)
    return filePath.toString()
  }
}