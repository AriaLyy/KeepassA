package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DbMergeDelegateBackgroundPendingContractTest {

  @Test
  fun backgroundConflictPersistsDownloadedSnapshotAndReturnsPending() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt").readText()

    assertTrue(source.contains("mergeInteractionMode == MergeInteractionMode.BACKGROUND"))
    assertTrue(source.contains("PendingMergeTaskStore("))
    assertTrue(source.contains("cloudSnapshot"))
    assertTrue(source.contains("return DbSynUtil.STATE_MERGE_PENDING"))
  }

  @Test
  fun compareInterceptorPassesDownloadedCloudFileAndRevisionToMergeDelegate() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt").readText()

    assertTrue(source.contains("cloudSnapshot = db"))
    assertTrue(source.contains("cloudModifiedTime = st.time"))
    assertTrue(source.contains("mergeInteractionMode = request.mergeInteractionMode"))
  }
}
