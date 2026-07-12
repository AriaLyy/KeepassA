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
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Date
import java.util.UUID
import timber.log.Timber

internal interface WebDavUploadClient {
  suspend fun getFileInfo(url: String): CloudFileInfo?

  suspend fun getSha256(url: String): String

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

  suspend fun move(
    sourceUrl: String,
    destinationUrl: String,
    overwrite: Boolean
  )

  suspend fun delete(url: String)
}

internal data class WebDavSafeUploadResult(
  val success: Boolean,
  val serviceModifyTime: Date? = null,
  val needsRecovery: Boolean = false
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
    val displacedOriginUrl = "$originUrl.kpa-replacing-$uploadId.tmp"
    val quarantineUrl = "$originUrl.kpa-quarantine-$uploadId.tmp"
    val expectedSize = localFile.length()
    val expectedSha256 = localFile.sha256()
    var tempMayExist = false
    var backupMayExist = false
    var backupReady = false
    var originPromotionAttempted = false
    var displacedOriginReady = false
    var originRestoredFromDisplaced = false
    var oldOriginSize: Long? = null
    var oldOriginSha256: String? = null
    var needsRecovery = false

    try {
      val oldOriginInfo = client.getFileInfo(originUrl)
      if (oldOriginInfo != null) {
        oldOriginSize = oldOriginInfo.size
        oldOriginSha256 = client.getSha256(originUrl)
        ensureBackupDirectory(backupDirUrl)
        backupMayExist = true
        client.copy(originUrl, backupUrl, true)
        val backupInfo = client.getFileInfo(backupUrl)
          ?: throw IllegalStateException("WebDAV backup missing: $backupUrl")
        if (backupInfo.size != oldOriginInfo.size) {
          throw IllegalStateException(
            "WebDAV backup size mismatch, expected=${oldOriginInfo.size}, actual=${backupInfo.size}"
          )
        }
        requireRemoteHash(backupUrl, oldOriginSha256)
        backupReady = true
      }

      tempMayExist = true
      client.put(tempUrl, localFile, WEB_DAV_DB_CONTENT_TYPE)
      val tempInfo = client.getFileInfo(tempUrl)
        ?: throw IllegalStateException("WebDAV temp upload missing: $tempUrl")
      if (tempInfo.size != expectedSize) {
        throw IllegalStateException("WebDAV temp upload size mismatch, expected=$expectedSize, actual=${tempInfo.size}")
      }
      requireRemoteHash(tempUrl, expectedSha256)

      originPromotionAttempted = true
      try {
        client.copy(tempUrl, originUrl, true)
      } catch (error: SardineException) {
        if (error.statusCode != 409 || oldOriginInfo == null) throw error
        client.move(originUrl, displacedOriginUrl, true)
        displacedOriginReady = true
        try {
          client.copy(tempUrl, originUrl, false)
        } catch (promotionError: Exception) {
          val partialOrigin = client.getFileInfo(originUrl)
          if (partialOrigin != null) {
            client.move(originUrl, quarantineUrl, true)
          }
          client.move(displacedOriginUrl, originUrl, false)
          val restored = client.getFileInfo(originUrl)
            ?: throw IllegalStateException("Restored moved-aside WebDAV origin missing: $originUrl")
          if (restored.size != oldOriginSize) {
            throw IllegalStateException("Restored moved-aside WebDAV origin size mismatch")
          }
          requireRemoteHash(originUrl, oldOriginSha256!!)
          displacedOriginReady = false
          originRestoredFromDisplaced = true
          if (partialOrigin != null) deleteQuietly(quarantineUrl)
          throw promotionError
        }
      }

      val originInfo = client.getFileInfo(originUrl)
        ?: throw IllegalStateException("WebDAV uploaded file missing: $originUrl")
      if (originInfo.size != expectedSize) {
        throw IllegalStateException("WebDAV uploaded file size mismatch, expected=$expectedSize, actual=${originInfo.size}")
      }
      requireRemoteHash(originUrl, expectedSha256)

      if (displacedOriginReady) {
        deleteQuietly(displacedOriginUrl)
        displacedOriginReady = false
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
        if (originPromotionAttempted) {
          val confirmed = runCatching {
            val originInfo = client.getFileInfo(originUrl) ?: return@runCatching null
            if (originInfo.size != expectedSize) return@runCatching null
            if (!client.getSha256(originUrl).equals(expectedSha256, ignoreCase = true)) {
              return@runCatching null
            }
            originInfo
          }.getOrNull()
          if (confirmed != null) {
            if (displacedOriginReady) {
              deleteQuietly(displacedOriginUrl)
              displacedOriginReady = false
            }
            deleteQuietly(tempUrl)
            tempMayExist = false
            if (backupReady) {
              pruneBackupsQuietly(backupDirUrl)
            }
            return WebDavSafeUploadResult(true, confirmed.serviceModifyDate)
          }
          if (backupReady && oldOriginSize != null && oldOriginSha256 != null) {
            val currentMatchesOld = runCatching {
              val current = client.getFileInfo(originUrl) ?: return@runCatching false
              current.size == oldOriginSize &&
                client.getSha256(originUrl).equals(oldOriginSha256, ignoreCase = true)
            }.getOrDefault(false)
            if (!currentMatchesOld) {
              val restored = runCatching {
                client.copy(backupUrl, originUrl, true)
                val restored = client.getFileInfo(originUrl)
                  ?: throw IllegalStateException("Restored WebDAV origin missing: $originUrl")
                if (restored.size != oldOriginSize) {
                  throw IllegalStateException("Restored WebDAV origin size mismatch")
                }
                requireRemoteHash(originUrl, oldOriginSha256)
              }.onFailure {
                Timber.e(it, "Restore verified WebDAV backup after uncertain promotion failed")
              }.isSuccess
              if (!restored) needsRecovery = true
            }
          }
        }
        // 网络挂了,任何远程操作都可能继续失败。保留 backup/temp 让下次上传或手动恢复兜底,
        // 避免在死连接上反复重试加剧故障。代价:留下 .tmp 孤儿文件,需要后续 preflight 清理。
        Timber.w("Network failure detected, skip remote rollback/cleanup; leave backup/temp for recovery")
        return WebDavSafeUploadResult(false, needsRecovery = needsRecovery)
      }
      if (originPromotionAttempted && backupReady) {
        if (originRestoredFromDisplaced) {
          // The verified old origin is already back in place.
        } else if (displacedOriginReady) {
          runCatching { client.move(displacedOriginUrl, originUrl, true) }
            .onSuccess { displacedOriginReady = false }
            .onFailure { Timber.e(it, "Restore moved-aside WebDAV origin failed") }
        } else {
          restoreBackupQuietly(backupUrl, originUrl)
        }
        pruneBackupsQuietly(backupDirUrl)
      } else if (backupMayExist) {
        deleteQuietly(backupUrl)
      }
      if (tempMayExist) {
        deleteQuietly(tempUrl)
      }
    }

    return WebDavSafeUploadResult(false, needsRecovery = needsRecovery)
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

  private suspend fun requireRemoteHash(url: String, expectedSha256: String) {
    val actual = client.getSha256(url)
    if (!actual.equals(expectedSha256, ignoreCase = true)) {
      throw IllegalStateException(
        "WebDAV content hash mismatch, url=$url, expected=$expectedSha256, actual=$actual"
      )
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
      client.copy(backupUrl, originUrl, true)
    }.onFailure {
      Timber.e(it, "Restore WebDAV backup failed, backupUrl=$backupUrl, originUrl=$originUrl")
    }
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

private fun File.sha256(): String {
  val digest = MessageDigest.getInstance("SHA-256")
  FileInputStream(this).use { input ->
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
      val count = input.read(buffer)
      if (count < 0) break
      digest.update(buffer, 0, count)
    }
  }
  return digest.digest().joinToString("") { "%02x".format(it) }
}
