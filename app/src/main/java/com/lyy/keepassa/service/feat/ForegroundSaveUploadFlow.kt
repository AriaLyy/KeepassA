package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.SynStateCode
import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal object ForegroundSaveUploadFlow {
  private val stateCode = object : SynStateCode {}

  suspend fun runLocalSave(
    needShowLoading: Boolean,
    minLoadingMs: Long,
    showLoading: () -> Unit,
    dismissLoading: (Long) -> Unit,
    save: suspend () -> Int,
    callback: (Int) -> Unit,
    emitSave: suspend () -> Unit,
    now: () -> Long = System::currentTimeMillis
  ) {
    val startTime = now()
    if (needShowLoading) showLoading()
    val code = try {
      save()
    } catch (error: CancellationException) {
      if (needShowLoading) dismissLoading(0L)
      callback(stateCode.STATE_CANCEL)
      throw error
    } catch (error: Throwable) {
      Timber.e(error, "前台保存失败")
      stateCode.STATE_FAIL
    }
    val elapsed = now() - startTime
    if (needShowLoading) {
      dismissLoading(if (elapsed < minLoadingMs) minLoadingMs else 0L)
    }
    callback(code)
    emitSave()
  }

  suspend fun run(
    needShowLoading: Boolean,
    minLoadingMs: Long,
    showLoading: () -> Unit,
    dismissLoading: (Long) -> Unit,
    upload: suspend ((Int) -> Unit) -> DbSyncResponse,
    callback: (Int) -> Unit,
    emitSave: suspend () -> Unit,
    now: () -> Long = System::currentTimeMillis
  ) {
    val completed = AtomicBoolean(false)
    val startTime = now()
    fun complete(code: Int, delay: Long) {
      if (!completed.compareAndSet(false, true)) {
        return
      }
      if (needShowLoading) {
        dismissLoading(delay)
      }
      callback(code)
    }

    if (needShowLoading) {
      showLoading()
    }
    val response = try {
      upload { code ->
        complete(code, 0L)
      }
    } catch (e: CancellationException) {
      complete(stateCode.STATE_CANCEL, 0L)
      com.lyy.keepassa.util.cloud.merge.MergeConflictSessionStore.completeResolvedSessions(stateCode.STATE_CANCEL)
      throw e
    } catch (e: Throwable) {
      Timber.e(e, "前台保存上传失败")
      DbSyncResponse(stateCode.STATE_FAIL, e.message.orEmpty())
    }
    Timber.i(response.msg)
    val endTime = now()
    complete(response.code, if ((endTime - startTime) < minLoadingMs) minLoadingMs else 0L)
    com.lyy.keepassa.util.cloud.merge.MergeConflictSessionStore.completeResolvedSessions(response.code)
    emitSave()
  }
}
