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
        ?: throw IOException("WebDAV temp upload missing: $tempUrl")
      if (tempInfo.size != expectedSize) {
        throw IOException("WebDAV temp upload size mismatch, expected=$expectedSize, actual=${tempInfo.size}")
      }

      originPutAttempted = true
      client.put(originUrl, localFile, WEB_DAV_DB_CONTENT_TYPE)

      val originInfo = client.getFileInfo(originUrl)
        ?: throw IOException("WebDAV uploaded file missing: $originUrl")
      if (originInfo.size != expectedSize) {
        throw IOException("WebDAV uploaded file size mismatch, expected=$expectedSize, actual=${originInfo.size}")
      }

      deleteQuietly(tempUrl)
      tempMayExist = false
      if (backupReady) {
        pruneBackupsQuietly(backupDirUrl)
      }
      return WebDavSafeUploadResult(true, originInfo.serviceModifyDate)
    } catch (e: Exception) {
      Timber.e(e, "WebDAV safe upload failed")
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
