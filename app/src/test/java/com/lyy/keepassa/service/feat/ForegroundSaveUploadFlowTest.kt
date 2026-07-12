package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.SynStateCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundSaveUploadFlowTest {
  private val stateCode = object : SynStateCode {}

  @Test
  fun runLocalSave_showsLoadingSavesAndDismissesWithoutUpload() = runBlocking {
    val events = mutableListOf<String>()

    ForegroundSaveUploadFlow.runLocalSave(
      needShowLoading = true,
      minLoadingMs = 200L,
      showLoading = { events += "show" },
      dismissLoading = { events += "dismiss:$it" },
      save = {
        events += "save-local"
        stateCode.STATE_SUCCEED
      },
      callback = { events += "callback:$it" },
      emitSave = { events += "emit-save" },
      now = increasingClock(1_000L, 1_050L)
    )

    assertEquals(
      listOf(
        "show",
        "save-local",
        "dismiss:200",
        "callback:${stateCode.STATE_SUCCEED}",
        "emit-save"
      ),
      events
    )
  }

  @Test
  fun run_dismissesLoadingAndCallbacksFailureWhenUploadThrows() = runBlocking {
    val events = mutableListOf<String>()

    ForegroundSaveUploadFlow.run(
      needShowLoading = true,
      minLoadingMs = 200L,
      showLoading = { events += "show" },
      dismissLoading = { events += "dismiss:$it" },
      upload = { _ -> error("network failed") },
      callback = { events += "callback:$it" },
      emitSave = { events += "save" },
      now = increasingClock(1_000L, 1_050L)
    )

    assertEquals(
      listOf(
        "show",
        "dismiss:200",
        "callback:${stateCode.STATE_FAIL}",
        "save"
      ),
      events
    )
  }

  @Test
  fun run_closesLoadingAndCallbacksCancelWhenCoroutineIsCancelled() = runBlocking {
    val events = mutableListOf<String>()

    runCatching {
      ForegroundSaveUploadFlow.run(
        needShowLoading = true,
        minLoadingMs = 200L,
        showLoading = { events += "show" },
        dismissLoading = { events += "dismiss:$it" },
        upload = { throw CancellationException("cancel") },
        callback = { events += "callback:$it" },
        emitSave = { events += "save" }
      )
    }

    assertEquals(
      listOf("show", "dismiss:0", "callback:${stateCode.STATE_CANCEL}"),
      events
    )
  }

  @Test
  fun run_allowsMergeFailureToCompleteForegroundCallbackBeforeUploadReturns() = runBlocking {
    val events = mutableListOf<String>()

    ForegroundSaveUploadFlow.run(
      needShowLoading = true,
      minLoadingMs = 200L,
      showLoading = { events += "show" },
      dismissLoading = { events += "dismiss:$it" },
      upload = { onMergeFailed ->
        onMergeFailed(stateCode.STATE_CANCEL)
        com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse(
          stateCode.STATE_CANCEL,
          "merge cancelled"
        )
      },
      callback = { events += "callback:$it" },
      emitSave = { events += "save" },
      now = increasingClock(1_000L, 1_050L)
    )

    assertEquals(
      listOf(
        "show",
        "dismiss:0",
        "callback:${stateCode.STATE_CANCEL}",
        "save"
      ),
      events
    )
  }

  private fun increasingClock(vararg values: Long): () -> Long {
    var index = 0
    return {
      values[index.coerceAtMost(values.lastIndex)].also {
        index++
      }
    }
  }
}
