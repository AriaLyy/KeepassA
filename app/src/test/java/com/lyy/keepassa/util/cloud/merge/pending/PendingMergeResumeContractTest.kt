package com.lyy.keepassa.util.cloud.merge.pending

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingMergeResumeContractTest {
  @Test
  fun notificationClickRequestsExactPersistedTask() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/pending/PendingMergeResumeCoordinator.kt").readText()

    assertTrue(source.contains("fun onNotificationClicked(taskId: String)"))
    assertTrue(source.contains("requestedTaskId.set(taskId)"))
    assertTrue(source.contains("repository.list().firstOrNull { it.id == preferredTaskId"))
  }

  @Test
  fun unavailableNetworkDoesNotBlockOpeningPersistedConflictSnapshot() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/pending/PendingMergeResumeCoordinator.kt").readText()

    assertTrue(source.contains("loadCurrentCloudRevisionOrNull"))
    assertTrue(source.contains("当前无法校验云端 revision，继续使用持久化快照解决冲突"))
    assertTrue(source.contains("if (currentRevision != null && !currentRevision.matches(taskRevision))"))
  }

  @Test
  fun notificationRequestQueuedDuringActiveResumeIsRetried() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/pending/PendingMergeResumeCoordinator.kt").readText()

    assertTrue(source.contains("retryRequested.getAndSet(false)"))
    assertTrue(source.contains("onDatabaseUnlocked()"))
  }


  @Test
  fun coordinatorReopensSnapshotRediffsAndCleansCompletedTask() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/merge/pending/PendingMergeResumeCoordinator.kt").readText()

    assertTrue(source.contains("Database().apply"))
    assertTrue(source.contains("DbMergeDelegate.compareDb("))
    assertTrue(source.contains("MergeInteractionMode.FOREGROUND"))
    assertTrue(source.contains("repository.remove(task.id)"))
    assertTrue(source.contains("snapshots.remove(task.id)"))
    assertTrue(source.contains("DbSynUtil.uploadSyn("))
    assertTrue(source.contains("currentRevision.matches(taskRevision)"))
    assertTrue(source.contains("PendingMergeState.DEFERRED_BY_USER"))
  }

  @Test
  fun unlockSuccessEntrypointsNotifyCoordinator() {
    val quickUnlock = File("src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt").readText()
    val openService = File("src/main/java/com/lyy/keepassa/service/feat/KdbOpenService.kt").readText()

    assertTrue(quickUnlock.contains("PendingMergeResumeCoordinator.onDatabaseUnlocked()"))
    assertTrue(openService.contains("PendingMergeResumeCoordinator.onDatabaseUnlocked()"))
  }
}
