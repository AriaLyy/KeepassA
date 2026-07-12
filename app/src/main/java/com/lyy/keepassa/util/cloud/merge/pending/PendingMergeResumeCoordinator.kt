package com.lyy.keepassa.util.cloud.merge.pending

import android.net.Uri
import android.text.TextUtils
import com.keepassdroid.Database
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.CloudUtilFactory
import com.lyy.keepassa.util.cloud.ICloudUtil
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.cloud.interceptor.CloudDatabaseOpenContext
import com.lyy.keepassa.util.cloud.interceptor.OpenedCloudDatabase
import com.lyy.keepassa.util.cloud.interceptor.DbMergeDelegate
import com.lyy.keepassa.util.cloud.interceptor.MergeInteractionMode
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.merge.MergeConflictSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object PendingMergeResumeCoordinator {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val resolving = AtomicBoolean(false)
  private val requestedTaskId = AtomicReference<String?>(null)
  private val retryRequested = AtomicBoolean(false)

  fun onNotificationClicked(taskId: String) {
    requestedTaskId.set(taskId)
    if (resolving.get()) retryRequested.set(true)
    onDatabaseUnlocked()
  }

  fun onDatabaseUnlocked() {
    if (BaseApp.isLocked || BaseApp.KDB?.pm == null || BaseApp.dbRecord == null) {
      return
    }
    if (!resolving.compareAndSet(false, true)) {
      return
    }
    scope.launch {
      try {
        resumeCurrentDatabase(requestedTaskId.get())
      } finally {
        MergeConflictSessionStore.completeResolvedSessions(DbSynUtil.STATE_FAIL)
        KpaUtil.kdbHandlerService.dismissMergeCompletionLoading()
        resolving.set(false)
        if (retryRequested.getAndSet(false)) {
          onDatabaseUnlocked()
        }
      }
    }
  }

  private suspend fun resumeCurrentDatabase(preferredTaskId: String?) {
    val record = BaseApp.dbRecord ?: return
    val pendingDirectory = File(BaseApp.APP.filesDir, PendingMergeSnapshotStore.ROOT_DIRECTORY)
    val repository = FilePendingMergeRepository(pendingDirectory)
    val snapshots = PendingMergeSnapshotStore(BaseApp.APP.filesDir)
    val task = preferredTaskId
      ?.let { id ->
        repository.list().firstOrNull { it.id == preferredTaskId && it.databaseIdentity == record.localDbUri }
      }
      ?: repository.findForDatabase(record.localDbUri)
      ?: return
    requestedTaskId.compareAndSet(task.id, null)
    val cloudUtil = CloudUtilFactory.getCloudUtil(record.getDbPathType())
    val currentRevision = loadCurrentCloudRevisionOrNull(record, cloudUtil)
    val taskRevision = CloudRevision(task.cloudModifiedTime, task.cloudContentHash)
    if (currentRevision != null && !currentRevision.matches(taskRevision)) {
      repository.remove(task.id)
      snapshots.remove(task.id)
      KpaUtil.kdbHandlerService.runForegroundUploadWithLoading {
        DbSynUtil.uploadSyn(
          record = record,
          mergeInteractionMode = MergeInteractionMode.FOREGROUND
        )
      }
      return
    }
    val snapshot = File(task.cloudSnapshotPath)
    if (!snapshot.isFile || snapshot.length() == 0L) {
      repository.remove(task.id)
      snapshots.remove(task.id)
      return
    }
    val localDatabase = BaseApp.KDB?.pm ?: return
    val cloudContext = CloudDatabaseOpenContext.create(BaseApp.APP)
    val cloudOwner = Database().apply {
      LoadDataStrict(
        cloudContext,
        Uri.fromFile(snapshot),
        QuickUnLockUtil.decryption(BaseApp.dbPass),
        if (TextUtils.isEmpty(BaseApp.dbKeyPath)) null else Uri.parse(
          QuickUnLockUtil.decryption(BaseApp.dbKeyPath)
        )
      )
    }
    if (cloudOwner.pm == null) {
      cloudContext.attachmentDirectory().deleteRecursively()
      return
    }
    val code = OpenedCloudDatabase(cloudOwner, cloudContext).use { opened ->
      DbMergeDelegate.compareDb(
        record = record,
        cloudDb = opened.database,
        localDb = localDatabase,
        isUpload = true,
        mergeInteractionMode = MergeInteractionMode.FOREGROUND,
        cloudSnapshot = snapshot,
        cloudModifiedTime = task.cloudModifiedTime
      )
    }
    if (code != DbSynUtil.STATE_SUCCEED) {
      if (code == DbSynUtil.STATE_CANCEL) {
        repository.save(task.copy(state = PendingMergeState.DEFERRED_BY_USER))
      }
      return
    }
    repository.remove(task.id)
    snapshots.remove(task.id)
    KpaUtil.kdbHandlerService.runForegroundUploadWithLoading {
      DbSynUtil.uploadSyn(
        record = record,
        mergeInteractionMode = MergeInteractionMode.FOREGROUND
      )
    }
  }

  private suspend fun loadCurrentCloudRevisionOrNull(
    record: DbHistoryRecord,
    cloudUtil: ICloudUtil
  ): CloudRevision? {
    return try {
      val path = record.cloudDiskPath.orEmpty()
      val currentInfo = cloudUtil.getFileInfo(path)
      CloudRevision(
        modifiedTime = cloudUtil.getFileServiceModifyTime(path).time,
        contentHash = currentInfo?.contentHash
      )
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Timber.w(error, "当前无法校验云端 revision，继续使用持久化快照解决冲突")
      null
    }
  }
}
