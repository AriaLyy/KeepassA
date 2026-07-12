package com.lyy.keepassa.util.cloud.merge.pending

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingMergeNotificationContractTest {

  @Test
  fun notificationUsesDedicatedChannelRequestCodeAndTaskIdExtra() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/pending/PendingMergeNotificationManager.kt").readText()

    assertTrue(source.contains("CHANNEL_ID_PENDING_MERGE"))
    assertTrue(source.contains("REQUEST_CODE_PENDING_MERGE"))
    assertTrue(source.contains("EXTRA_PENDING_MERGE_TASK_ID"))
    assertTrue(source.contains("PendingIntent.FLAG_UPDATE_CURRENT"))
    assertTrue(source.contains("Manifest.permission.POST_NOTIFICATIONS"))
  }

  @Test
  fun notificationTargetsConsumeTaskIdForFreshAndReusedActivities() {
    val quickUnlock = File("src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt").readText()
    val launcher = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt").readText()

    assertTrue(quickUnlock.contains("handlePendingMergeNotificationIntent(intent)"))
    assertTrue(quickUnlock.substringAfter("override fun onNewIntent").contains("handlePendingMergeNotificationIntent(intent)"))
    assertTrue(launcher.contains("handlePendingMergeNotificationIntent(intent)"))
    assertTrue(launcher.substringAfter("override fun onNewIntent").contains("handlePendingMergeNotificationIntent(intent)"))
  }

  @Test
  fun backgroundConflictDispatchesNotificationAfterTaskPersistence() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt").readText()

    assertTrue(source.contains("val pendingTask = PendingMergeTaskStore("))
    assertTrue(source.contains("PendingMergeNotificationManager.notify(pendingTask.id)"))
  }
}
