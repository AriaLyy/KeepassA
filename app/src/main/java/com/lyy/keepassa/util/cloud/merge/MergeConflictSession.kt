package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDataInf
import com.lyy.keepassa.util.cloud.SynStateCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val syncStateCode = object : SynStateCode {}

data class MergeConflictItem(
  val cloud: PwDataInf,
  val local: PwDataInf,
  val autoMerge: AutoMergeResult
)

data class MergeConflictSession(
  val id: String = UUID.randomUUID().toString(),
  val items: List<MergeConflictItem>,
  val localOnlyItems: List<PwDataInf>,
  val channel: Channel<MergeConflictResult> = Channel(Channel.CONFLATED),
  val onMergeFailed: ((Int) -> Unit)? = null
) {
  private val completed = AtomicBoolean(false)
  val processingCompleted = CompletableDeferred<Int>()

  fun tryComplete(result: MergeConflictResult): Boolean {
    if (!completed.compareAndSet(false, true)) {
      return false
    }
    if (result !is MergeConflictResult.Resolved) {
      onMergeFailed?.invoke(result.syncCode)
    }
    return channel.trySend(result).isSuccess
  }

  fun completeProcessing(code: Int) {
    processingCompleted.complete(code)
  }
}

sealed class MergeConflictResult {
  data class Resolved(
    val decisions: Map<Int, Map<FieldKey, Decision>>,
    val deleteLocalOnlyIndexes: Set<Int>
  ) : MergeConflictResult()

  object Cancelled : MergeConflictResult()
}

val MergeConflictResult.syncCode: Int
  get() = when (this) {
    is MergeConflictResult.Resolved -> syncStateCode.STATE_SUCCEED
    MergeConflictResult.Cancelled -> syncStateCode.STATE_CANCEL
  }

object MergeConflictSessionStore {
  private val sessions = ConcurrentHashMap<String, MergeConflictSession>()

  fun put(session: MergeConflictSession) {
    sessions[session.id] = session
  }

  fun get(id: String): MergeConflictSession? = sessions[id]

  fun remove(id: String): MergeConflictSession? = sessions.remove(id)

  fun completeResolvedSessions(code: Int) {
    sessions.values
      .filter { it.processingCompleted.isActive }
      .forEach { it.completeProcessing(code) }
  }
}
