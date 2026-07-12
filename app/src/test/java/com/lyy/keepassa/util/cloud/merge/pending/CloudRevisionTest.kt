package com.lyy.keepassa.util.cloud.merge.pending

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudRevisionTest {

  @Test
  fun matchingRevisionAcceptsSameTimeAndHash() {
    val expected = CloudRevision(10L, "hash")

    assertTrue(expected.matches(CloudRevision(10L, "hash")))
    assertFalse(expected.matches(CloudRevision(11L, "hash")))
    assertFalse(expected.matches(CloudRevision(10L, "other")))
  }

  @Test
  fun missingHashFallsBackToModificationTime() {
    assertTrue(CloudRevision(10L, null).matches(CloudRevision(10L, "server-hash")))
    assertFalse(CloudRevision(10L, null).matches(CloudRevision(11L, "server-hash")))
  }
}
