package com.lyy.keepassa.util.cloud.interceptor

import android.content.Context
import android.net.Uri
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.ICloudUtil
import com.lyy.keepassa.util.cloud.SynStateCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Date

class DbSyncUploadInterceptorTest {

  @Test
  fun intercept_notifiesUserToRetryLaterWhenUploadFails() = runBlocking {
    val record = record()
    val notifiedRecords = mutableListOf<DbHistoryRecord>()
    val interceptor = DbSyncUploadInterceptor(
      contextProvider = { mockk<Context>(relaxed = true) },
      uploadFailureNotifier = { notifiedRecords += it }
    )

    val response = interceptor.intercept(
      DbSyncRequest(
        record = record,
        syncUtil = FakeCloudUtil(uploadResult = false),
        interceptors = listOf(interceptor)
      )
    )

    assertEquals((object : SynStateCode {}).STATE_FAIL, response.code)
    assertEquals(1, notifiedRecords.size)
    assertSame(record, notifiedRecords.single())
  }

  private fun record(): DbHistoryRecord {
    return DbHistoryRecord(
      time = 1L,
      type = "WEBDAV",
      localDbUri = "file:///local.kdbx",
      cloudDiskPath = "/remote.kdbx",
      keyUri = "",
      dbName = "local.kdbx"
    )
  }

  private class FakeCloudUtil(
    private val uploadResult: Boolean
  ) : ICloudUtil {
    override suspend fun fileExists(fileKey: String): Boolean = false
    override fun getRootPath(): String = "/"
    override suspend fun getFileList(dirPath: String): List<CloudFileInfo>? = emptyList()
    override suspend fun checkContentHash(cloudFileHash: String?, localFileUri: Uri): Boolean = false
    override suspend fun getFileInfo(fileKey: String): CloudFileInfo? = null
    override suspend fun delFile(fileKey: String): Boolean = false
    override suspend fun getFileServiceModifyTime(fileKey: String): Date = Date(0)
    override suspend fun uploadFile(context: Context, dbRecord: DbHistoryRecord): Boolean = uploadResult
    override suspend fun downloadFile(context: Context, dbRecord: DbHistoryRecord, filePath: Uri): String? = null
  }
}
