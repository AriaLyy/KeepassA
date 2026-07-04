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
  val serviceModifyTime: Date? = null
)

internal class WebDavSafeUploader(
  private val client: WebDavUploadClient,
  private val idFactory: () -> String = { "${System.currentTimeMillis()}-${UUID.randomUUID()}" }
) {

  suspend fun upload(
    localFile: File,
    originUrl: String
  ): WebDavSafeUploadResult {
    val uploadId = idFactory()
    val tempUrl = "$originUrl.kpa-uploading-$uploadId.tmp"
    val backupUrl = "$originUrl.kpa-backup-$uploadId.bak"
    val expectedSize = localFile.length()
    var tempMayExist = false
    var backupCreated = false
    var originMayBeChanged = false

    try {
      if (client.getFileInfo(originUrl) != null) {
        client.copy(originUrl, backupUrl, true)
        backupCreated = true
      }

      tempMayExist = true
      client.put(tempUrl, localFile, WEB_DAV_DB_CONTENT_TYPE)
      val tempInfo = client.getFileInfo(tempUrl)
        ?: throw IOException("WebDAV temp upload missing: $tempUrl")
      if (tempInfo.size != expectedSize) {
        throw IOException("WebDAV temp upload size mismatch, expected=$expectedSize, actual=${tempInfo.size}")
      }

      originMayBeChanged = true
      moveReplacing(tempUrl, originUrl, allowDeleteDestinationFallback = backupCreated)
      tempMayExist = false

      val originInfo = client.getFileInfo(originUrl)
        ?: throw IOException("WebDAV uploaded file missing: $originUrl")
      if (originInfo.size != expectedSize) {
        throw IOException("WebDAV uploaded file size mismatch, expected=$expectedSize, actual=${originInfo.size}")
      }

      if (backupCreated) {
        deleteQuietly(backupUrl)
      }
      return WebDavSafeUploadResult(true, originInfo.serviceModifyDate)
    } catch (e: Exception) {
      Timber.e(e, "WebDAV safe upload failed")
      if (originMayBeChanged && backupCreated) {
        restoreBackupQuietly(backupUrl, originUrl)
      } else if (backupCreated) {
        deleteQuietly(backupUrl)
      }
      if (tempMayExist) {
        deleteQuietly(tempUrl)
      }
    }

    return WebDavSafeUploadResult(false)
  }

  private suspend fun restoreBackupQuietly(
    backupUrl: String,
    originUrl: String
  ) {
    runCatching {
      moveReplacing(backupUrl, originUrl, allowDeleteDestinationFallback = true)
    }.onFailure {
      Timber.e(it, "Restore WebDAV backup failed, backupUrl=$backupUrl, originUrl=$originUrl")
    }
  }

  private suspend fun moveReplacing(
    sourceUrl: String,
    destinationUrl: String,
    allowDeleteDestinationFallback: Boolean
  ) {
    try {
      client.move(sourceUrl, destinationUrl, true)
      return
    } catch (e: Exception) {
      if (!allowDeleteDestinationFallback || !isMoveConflict(e)) {
        throw e
      }
      Timber.w(
        e,
        "WebDAV MOVE overwrite conflicted, retry after deleting destination, sourceUrl=$sourceUrl, destinationUrl=$destinationUrl"
      )
    }
    client.delete(destinationUrl)
    client.move(sourceUrl, destinationUrl, true)
  }

  private fun isMoveConflict(error: Throwable): Boolean {
    return error is SardineException && error.statusCode == 409
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
  }
}
