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
import com.lyy.keepassa.entity.DbHistoryRecord
import java.util.Date

/**
 * 云盘工具
 */
interface ICloudUtil {

  /**
   * 获取云盘根路径
   */
  fun getRootPath(): String

  /**
   * 获取文件列表
   * @return 如果获取不到文件列表，返回null
   */
  suspend fun getFileList(path: String): List<CloudFileInfo>?

  /**
   * 检查云端文件的hash和本地文件的hash是否一致
   * @param cloudFileHash 云端文件的hash
   * @param localFileUri 本地文件的Uri
   * @return true 两端文件一致
   */
  suspend fun checkContentHash(
    cloudFileHash: String,
    localFileUri: Uri
  ): Boolean

  /**
   * 获取云端文件信息
   * @param fileKey webDav/dropbox中为云端路径，onedrive为id
   * @return null 云端文件不存在
   */
  suspend fun getFileInfo(fileKey: String): CloudFileInfo?

  /**
   * 删除文件
   * @param fileKey webDav/dropbox中为云端路径，onedrive为id
   * @return true 删除成功
   */
  suspend fun delFile(fileKey: String): Boolean

  /**
   * 云端文件的修改时间
   * @param fileKey 云端文件路径
   */
  suspend fun getFileServiceModifyTime(fileKey: String): Date

  /**
   * 上传文件，上传完成需要更新[DbSynUtil.serviceModifyTime]
   * @param dbRecord 文件打开记录
   * @return true 上传成功
   */
  suspend fun uploadFile(
    context: Context,
    dbRecord: DbHistoryRecord
  ): Boolean

  /**
   * 下载文件
   * @param filePath 文件保存路径
   * @return 文件保存路径，null 表示下载失败
   */
  suspend fun downloadFile(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String?

}