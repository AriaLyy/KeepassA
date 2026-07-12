package com.lyy.keepassa.util.cloud

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudPathUploadLockTest {
  @Test
  fun sameCloudPathRunsOneUploadTransactionAtATime() = runBlocking {
    val active = AtomicInteger(0)
    val maxActive = AtomicInteger(0)

    suspend fun transaction() = CloudPathUploadLock.withLock("https://dav/db.kdbx") {
      val now = active.incrementAndGet()
      maxActive.updateAndGet { maxOf(it, now) }
      delay(30)
      active.decrementAndGet()
    }

    val first = async { transaction() }
    val second = async { transaction() }
    first.await()
    second.await()

    assertEquals(1, maxActive.get())
  }
}
