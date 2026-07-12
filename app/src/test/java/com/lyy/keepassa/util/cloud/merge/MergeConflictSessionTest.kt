package com.lyy.keepassa.util.cloud.merge

import com.lyy.keepassa.util.cloud.SynStateCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeConflictSessionTest {
  private val stateCode = object : SynStateCode {}

  @Test
  fun resolvedSessionWaitsForProcessingCompletionBeforeUiCanClose() = runBlocking {
    val session = MergeConflictSession(items = emptyList(), localOnlyItems = emptyList())

    session.tryComplete(MergeConflictResult.Resolved(emptyMap(), emptySet()))
    assertTrue(!session.processingCompleted.isCompleted)

    session.completeProcessing(stateCode.STATE_SUCCEED)
    assertEquals(stateCode.STATE_SUCCEED, session.processingCompleted.await())
  }

  @Test
  fun resultChannelBuffersCancelUntilDelegateReceives() = runBlocking {
    val session = MergeConflictSession(
      items = emptyList(),
      localOnlyItems = emptyList()
    )

    assertTrue(session.tryComplete(MergeConflictResult.Cancelled))

    assertSame(MergeConflictResult.Cancelled, session.channel.receive())
  }

  @Test
  fun cancelledResultMapsToSyncCancelState() {
    assertEquals(stateCode.STATE_CANCEL, MergeConflictResult.Cancelled.syncCode)
  }

  @Test
  fun tryComplete_callsMergeFailureCallbackWhenCancelled() {
    val callbacks = mutableListOf<Int>()
    val session = MergeConflictSession(
      items = emptyList(),
      localOnlyItems = emptyList(),
      onMergeFailed = { callbacks += it }
    )

    assertTrue(session.tryComplete(MergeConflictResult.Cancelled))

    assertEquals(listOf(stateCode.STATE_CANCEL), callbacks)
  }

  @Test
  fun tryComplete_doesNotCallMergeFailureCallbackForResolvedResult() {
    val callbacks = mutableListOf<Int>()
    val session = MergeConflictSession(
      items = emptyList(),
      localOnlyItems = emptyList(),
      onMergeFailed = { callbacks += it }
    )

    assertTrue(
      session.tryComplete(
        MergeConflictResult.Resolved(
          decisions = emptyMap(),
          deleteLocalOnlyIndexes = emptySet()
        )
      )
    )

    assertTrue(callbacks.isEmpty())
  }
}
