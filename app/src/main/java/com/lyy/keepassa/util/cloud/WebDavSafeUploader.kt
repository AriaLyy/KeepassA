/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud

import com.thegrizzlylabs.sardineandroid.impl.SardineException
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.UUID
import timber.log.Timber

internal interface WebDavUploadClient {
  suspend fun getFileInfo(url: String): CloudFileInfo?

  suspend fun listFiles(url: String): List<CloudFileInfo>

  suspend fun createDirectory(url: String)

  suspend fun put(
    url: String,
    localFile: File,
    contentType: String
  )

  suspend fun copy(
    sourceUrl: String,
    destinationUrl: String,
    overwrite: Boolean
  )

  suspend fun delete(url: String)
}

internal data class WebDavSafeUploadResult(
  val success: Boolean,
  val serviceModifyTime: Date? = null
)

internal class WebDavSafeUploader(
  private val client: WebDavUploadClient,
  private val idFactory: () -> String = { "${System.currentTimeMillis()}-${UUID.randomUUID()}" },
  private val maxBackupCount: Int = MAX_BACKUP_COUNT
) {

  suspend fun upload(
    localFile: File,
    originUrl: String
  ): WebDavSafeUploadResult {
    val uploadId = idFactory()
    val originFileName = originUrl.substringAfterLast('/')
    val tempUrl = "$originUrl.kpa-uploading-$uploadId.tmp"
    val backupDirUrl = "$originUrl.bak/"
    val backupUrl = "$backupDirUrl$uploadId-$originFileName"
    val expectedSize = localFile.length()
    var tempMayExist = false
    var backupMayExist = false
    var backupReady = false
    var originPutAttempted = false

    try {
      if (client.getFileInfo(originUrl) != null) {
        ensureBackupDirectory(backupDirUrl)
        backupMayExist = true
        client.copy(originUrl, backupUrl, true)
        backupReady = true
      }

      tempMayExist = true
      client.put(tempUrl, localFile, WEB_DAV_DB_CONTENT_TYPE)
      val tempInfo = client.getFileInfo(tempUrl)
        ?: throw IllegalStateException("WebDAV temp upload missing: $tempUrl")
      if (tempInfo.size != expectedSize) {
        throw IllegalStateException("WebDAV temp upload size mismatch, expected=$expectedSize, actual=${tempInfo.size}")
      }

      originPutAttempted = true
      client.put(originUrl, localFile, WEB_DAV_DB_CONTENT_TYPE)

      val originInfo = client.getFileInfo(originUrl)
        ?: throw IllegalStateException("WebDAV uploaded file missing: $originUrl")
      if (originInfo.size != expectedSize) {
        throw IllegalStateException("WebDAV uploaded file size mismatch, expected=$expectedSize, actual=${originInfo.size}")
      }

      deleteQuietly(tempUrl)
      tempMayExist = false
      if (backupReady) {
        pruneBackupsQuietly(backupDirUrl)
      }
      return WebDavSafeUploadResult(true, originInfo.serviceModifyDate)
    } catch (e: Exception) {
      Timber.e(e, "WebDAV safe upload failed")
      if (isNetworkFailure(e)) {
        // 网络挂了,任何远程操作都可能继续失败。保留 backup/temp 让下次上传或手动恢复兜底,
        // 避免在死连接上反复重试加剧故障。代价:留下 .tmp 孤儿文件,需要后续 preflight 清理。
        Timber.w("Network failure detected, skip remote rollback/cleanup; leave backup/temp for recovery")
        return WebDavSafeUploadResult(false)
      }
      if (originPutAttempted && backupReady) {
        restoreBackupQuietly(backupUrl, originUrl)
        pruneBackupsQuietly(backupDirUrl)
      } else if (backupMayExist) {
        deleteQuietly(backupUrl)
      }
      if (tempMayExist) {
        deleteQuietly(tempUrl)
      }
    }

    return WebDavSafeUploadResult(false)
  }

  /**
   * 判断异常是否属于"网络层故障"——应跳过所有远程回滚/清理。
   *
   * 区分依据:
   * - [SardineException]:服务器正常返回了 HTTP 错误(4xx/5xx),传输层没坏 → 不是网络故障,可回滚
   * - [IllegalStateException]:我们自己在 put 后做元数据校验时抛出的(server 返回的 size 不对、文件丢失等),
   *   表明服务器通讯正常但内容不一致 → 不是网络故障,必须回滚
   * - [IOException]:OkHttp/Sardine 在传输层(SocketTimeout/Connect/StreamReset 等)抛出 → 网络故障,跳过回滚
   */
  private fun isNetworkFailure(e: Throwable): Boolean {
    var cur: Throwable? = e
    while (cur != null) {
      if (cur is SardineException) return false
      if (cur is IllegalStateException) return false
      if (cur is IOException) return true
      cur = cur.cause
    }
    return false
  }

  private suspend fun ensureBackupDirectory(backupDirUrl: String) {
    if (client.getFileInfo(backupDirUrl) != null) {
      return
    }
    runCatching {
      client.createDirectory(backupDirUrl)
    }.onFailure {
      if (client.getFileInfo(backupDirUrl) == null) {
        throw it
      }
    }
  }

  private suspend fun pruneBackupsQuietly(backupDirUrl: String) {
    runCatching {
      val backups = client.listFiles(backupDirUrl)
        .filter { !it.isDir && it.fileKey.startsWith(backupDirUrl) }
        .sortedWith(
          compareByDescending<CloudFileInfo> { backupSortKey(it) }
            .thenByDescending { it.fileName }
        )
      backups.drop(maxBackupCount).forEach {
        deleteQuietly(it.fileKey)
      }
    }.onFailure {
      Timber.e(it, "Prune WebDAV backups failed, backupDirUrl=$backupDirUrl")
    }
  }

  private suspend fun restoreBackupQuietly(
    backupUrl: String,
    originUrl: String
  ) {
    runCatching {
      copyReplacing(backupUrl, originUrl, allowDeleteDestinationFallback = true)
    }.onFailure {
      Timber.e(it, "Restore WebDAV backup failed, backupUrl=$backupUrl, originUrl=$originUrl")
    }
  }

  private suspend fun copyReplacing(
    sourceUrl: String,
    destinationUrl: String,
    allowDeleteDestinationFallback: Boolean
  ) {
    try {
      client.copy(sourceUrl, destinationUrl, true)
      return
    } catch (e: Exception) {
      if (!allowDeleteDestinationFallback || !isMoveConflict(e)) {
        throw e
      }
      Timber.w(
        e,
        "WebDAV COPY overwrite conflicted, retry after deleting destination, sourceUrl=$sourceUrl, destinationUrl=$destinationUrl"
      )
    }
    client.delete(destinationUrl)
    client.copy(sourceUrl, destinationUrl, true)
  }

  private fun isMoveConflict(error: Throwable): Boolean {
    return error is SardineException && error.statusCode == 409
  }

  private fun backupSortKey(info: CloudFileInfo): Long {
    return info.fileName.substringBefore('-').toLongOrNull() ?: info.serviceModifyDate.time
  }

  private suspend fun deleteQuietly(url: String) {
    runCatching {
      client.delete(url)
    }.onFailure {
      Timber.e(it, "Delete WebDAV temp file failed, url=$url")
    }
  }

  private companion object {
    const val WEB_DAV_DB_CONTENT_TYPE = "application/binary"
    const val MAX_BACKUP_COUNT = 10
  }
}
