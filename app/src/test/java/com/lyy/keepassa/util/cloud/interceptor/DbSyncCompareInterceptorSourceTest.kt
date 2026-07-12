package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DbSyncCompareInterceptorSourceTest {

  @Test
  fun compareInterceptorStopsUploadForAnyNonSuccessMergeResult() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt").readText()

    assertTrue(source.contains("if (code != DbSynUtil.STATE_SUCCEED)"))
  }

  @Test
  fun compareInterceptorDoesNotCacheRequestsWithPerSyncCallbacks() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt").readText()

    assertTrue(source.contains("mergeFailureCallback = request.mergeFailureCallback"))
    assertTrue(!source.contains("private var nextRequest"))
  }

  @Test
  fun unchangedRemoteTimestampStillChecksDatabaseContentBeforeUploading() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt").readText()
    val unchangedRemoteBranch = source.substringAfter("if (st == DbSynUtil.serviceModifyTime)")
      .substringBefore("Timber.i(\n      \"云端文件修改时间")

    assertTrue(unchangedRemoteBranch.contains("downloadCloudDatabase(request)"))
    assertTrue(unchangedRemoteBranch.contains("DatabaseContentComparator().sameContent"))
    assertTrue(unchangedRemoteBranch.contains("return normal(DbSynUtil.STATE_SUCCEED"))
  }
}
