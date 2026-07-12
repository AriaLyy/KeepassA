package com.lyy.keepassa.util.cloud

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object CloudPathUploadLock {
  private val locks = ConcurrentHashMap<String, Mutex>()

  suspend fun <T> withLock(cloudPath: String, action: suspend () -> T): T {
    val key = cloudPath.trim()
    return locks.computeIfAbsent(key) { Mutex() }.withLock { action() }
  }
}
