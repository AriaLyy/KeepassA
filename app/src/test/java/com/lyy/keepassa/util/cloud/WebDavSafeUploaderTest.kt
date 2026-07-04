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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebDavSafeUploaderTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private val originUrl = "https://dav.example.com/keepass/db.kdbx"
  private val tempUrl = "$originUrl.kpa-uploading-fixed.tmp"
  private val backupDirUrl = "$originUrl.bak/"
  private val backupUrl = "${backupDirUrl}fixed-db.kdbx"

  @Test fun putTempFailure_keepsOriginAndDeletesBackupAndTemp() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      failPutUrls = setOf(tempUrl),
      createPartialOnPutFailure = true
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    assertFalse(client.files.containsKey(tempUrl))
    assertFalse(client.files.containsKey(backupUrl))
    assertTrue(client.operations.contains("delete:$tempUrl"))
    assertTrue(client.operations.contains("delete:$backupUrl"))
    assertFalse(client.operations.contains("put:$originUrl:20"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun putTempNetworkFailure_skipsRollbackLeavesBackupAndTempForRecovery() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      networkFailPutUrls = setOf(tempUrl),
      createPartialOnPutFailure = true
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    // 网络故障:跳过回滚,backup 和 temp 都保留给下次启动清理
    assertTrue(client.files.containsKey(backupUrl))
    assertTrue(client.files.containsKey(tempUrl))
    assertFalse(client.operations.contains("delete:$tempUrl"))
    assertFalse(client.operations.contains("delete:$backupUrl"))
    assertFalse(client.operations.contains("put:$originUrl:20"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun originInfoFailure_abortsBeforeChangingRemoteFiles() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      failInfoUrls = setOf(originUrl)
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    assertFalse(client.files.containsKey(tempUrl))
    assertFalse(client.files.containsKey(backupUrl))
    assertEquals(listOf("info:$originUrl"), client.operations)
  }

  @Test fun tempSizeMismatch_doesNotReplaceOriginAndDeletesPreflightBackup() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      uploadedSizeOverride = mapOf(tempUrl to 19)
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    assertFalse(client.files.containsKey(tempUrl))
    assertFalse(client.files.containsKey(backupUrl))
    assertFalse(client.operations.contains("put:$originUrl:20"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun finalPutFailure_restoresOriginFromBackupAndKeepsBackup() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      failPutUrls = setOf(originUrl),
      createPartialOnPutFailure = true
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    assertFalse(client.files.containsKey(tempUrl))
    assertTrue(client.files.containsKey(backupUrl))
    assertTrue(client.operations.contains("copy:$backupUrl->$originUrl:true"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun finalPutNetworkFailure_skipsRollbackLeavesBackupAndTempForRecovery() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      networkFailPutUrls = setOf(originUrl),
      createPartialOnPutFailure = true
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    // 网络故障:跳过回滚。origin 可能存在 partial 坏数据,backup 保留可手动恢复,temp 也保留
    assertTrue(client.files.containsKey(backupUrl))
    assertTrue(client.files.containsKey(tempUrl))
    assertFalse(client.operations.contains("delete:$tempUrl"))
    assertFalse(client.operations.contains("delete:$backupUrl"))
    assertFalse(client.operations.contains("copy:$backupUrl->$originUrl"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun restoreCopyConflict_deletesOriginThenCopiesBackup() = runBlocking {
    val oldInfo = cloudInfo(originUrl, size = 10, time = 1000)
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to oldInfo),
      failPutUrls = setOf(originUrl),
      createPartialOnPutFailure = true,
      copyConflictWhenDestinationExists = true
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertFalse(result.success)
    assertEquals(oldInfo, client.files[originUrl])
    assertTrue(client.files.containsKey(backupUrl))
    assertTrue(client.operations.contains("copy:$backupUrl->$originUrl:true"))
    assertTrue(client.operations.contains("delete:$originUrl"))
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  @Test fun success_putsOriginKeepsBackupAndReturnsModifyTime() = runBlocking {
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf(originUrl to cloudInfo(originUrl, size = 10, time = 1000)),
      uploadedTime = 3000
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertTrue(result.success)
    assertEquals(20L, client.files[originUrl]?.size)
    assertEquals(Date(3000), result.serviceModifyTime)
    assertFalse(client.files.containsKey(tempUrl))
    assertTrue(client.files.containsKey(backupDirUrl))
    assertTrue(client.files.containsKey(backupUrl))
    assertFalse(client.operations.any { it.startsWith("move:") })
    assertFalse(client.operations.contains("delete:$originUrl"))
    assertEquals(
      listOf(
        "info:$originUrl",
        "info:$backupDirUrl",
        "mkdir:$backupDirUrl",
        "copy:$originUrl->$backupUrl:true",
        "put:$tempUrl:20",
        "info:$tempUrl",
        "put:$originUrl:20",
        "info:$originUrl",
        "delete:$tempUrl",
        "list:$backupDirUrl"
      ),
      client.operations
    )
  }

  @Test fun success_prunesBackupDirectoryToLatestTen() = runBlocking {
    val oldBackups = (0 until 10).associate { index ->
      val url = "${backupDirUrl}old-$index-db.kdbx"
      url to cloudInfo(url, size = 10, time = 1000L + index)
    }
    val newestBackupUrl = "${backupDirUrl}9999-db.kdbx"
    val client = FakeWebDavUploadClient(
      initialFiles = mutableMapOf<String, CloudFileInfo>().apply {
        put(originUrl, cloudInfo(originUrl, size = 10, time = 1000))
        put(backupDirUrl, cloudInfo(backupDirUrl, size = 0, time = 1000, isDir = true))
        putAll(oldBackups)
      },
      uploadedTime = 3000
    )
    val uploader = WebDavSafeUploader(client, idFactory = { "9999" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertTrue(result.success)
    assertTrue(client.files.containsKey(newestBackupUrl))
    assertFalse(client.files.containsKey("${backupDirUrl}old-0-db.kdbx"))
    val backups = client.files.values.filter { it.fileKey.startsWith(backupDirUrl) && !it.isDir }
    assertEquals(10, backups.size)
  }

  @Test fun success_whenOriginMissing_skipsBackupDirectoryAndPutsOrigin() = runBlocking {
    val client = FakeWebDavUploadClient(uploadedTime = 3000)
    val uploader = WebDavSafeUploader(client, idFactory = { "fixed" })

    val result = uploader.upload(localFile(size = 20), originUrl)

    assertTrue(result.success)
    assertEquals(20L, client.files[originUrl]?.size)
    assertEquals(Date(3000), result.serviceModifyTime)
    assertFalse(client.files.containsKey(backupDirUrl))
    assertFalse(client.files.containsKey(backupUrl))
    assertFalse(client.operations.any { it.startsWith("copy:") })
    assertFalse(client.operations.any { it.startsWith("move:") })
  }

  private fun localFile(size: Int): File {
    val file = tempFolder.newFile()
    file.writeBytes(ByteArray(size) { 1 })
    return file
  }

  private fun cloudInfo(
    url: String,
    size: Long,
    time: Long,
    isDir: Boolean = false
  ): CloudFileInfo {
    return CloudFileInfo(
      fileKey = url,
      fileName = url.trimEnd('/').substringAfterLast('/'),
      serviceModifyDate = Date(time),
      size = size,
      isDir = isDir
    )
  }

  private inner class FakeWebDavUploadClient(
    initialFiles: MutableMap<String, CloudFileInfo> = mutableMapOf(),
    private val failPutUrls: Set<String> = emptySet(),
    private val networkFailPutUrls: Set<String> = emptySet(),
    private val uploadedSizeOverride: Map<String, Long> = emptyMap(),
    private val uploadedTime: Long = 2000,
    private val createPartialOnPutFailure: Boolean = false,
    private val failInfoUrls: Set<String> = emptySet(),
    private val copyConflictWhenDestinationExists: Boolean = false
  ) : WebDavUploadClient {

    val files = initialFiles
    val operations = mutableListOf<String>()

    override suspend fun getFileInfo(url: String): CloudFileInfo? {
      operations.add("info:$url")
      if (url in failInfoUrls) {
        throw IOException("info failed")
      }
      return files[url]
    }

    override suspend fun listFiles(url: String): List<CloudFileInfo> {
      operations.add("list:$url")
      return files.values
        .filter { it.fileKey == url || it.fileKey.startsWith(url) }
        .sortedBy { it.fileKey }
    }

    override suspend fun createDirectory(url: String) {
      operations.add("mkdir:$url")
      files[url] = cloudInfo(url, size = 0, time = uploadedTime, isDir = true)
    }

    override suspend fun put(
      url: String,
      localFile: File,
      contentType: String
    ) {
      operations.add("put:$url:${localFile.length()}")
      when {
        url in failPutUrls -> {
          // 服务器级失败(HTTP 5xx):传输层正常,触发回滚路径
          if (createPartialOnPutFailure) {
            files[url] = cloudInfo(url, size = 3, time = uploadedTime)
          }
          throw SardineException("server put failed", 500, "")
        }
        url in networkFailPutUrls -> {
          // 传输层故障(SocketTimeout/StreamReset 等):触发跳过回滚路径
          if (createPartialOnPutFailure) {
            files[url] = cloudInfo(url, size = 3, time = uploadedTime)
          }
          throw IOException("network put failed")
        }
        else -> {
          files[url] = cloudInfo(url, uploadedSizeOverride[url] ?: localFile.length(), uploadedTime)
        }
      }
    }

    override suspend fun copy(
      sourceUrl: String,
      destinationUrl: String,
      overwrite: Boolean
    ) {
      operations.add("copy:$sourceUrl->$destinationUrl:$overwrite")
      if (
        copyConflictWhenDestinationExists &&
        destinationUrl == originUrl &&
        files.containsKey(destinationUrl)
      ) {
        throw SardineException("conflict", 409, "")
      }
      val source = files[sourceUrl]!!
      files[destinationUrl] = cloudInfo(
        destinationUrl,
        size = source.size,
        time = source.serviceModifyDate.time,
        isDir = source.isDir
      )
    }

    override suspend fun delete(url: String) {
      operations.add("delete:$url")
      files.remove(url)
    }
  }
}
