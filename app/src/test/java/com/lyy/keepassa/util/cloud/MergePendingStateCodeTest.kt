package com.lyy.keepassa.util.cloud

import org.junit.Assert.assertNotEquals
import org.junit.Test

class MergePendingStateCodeTest {

  @Test
  fun mergePendingHasDedicatedNonSuccessStateCode() {
    val codes = object : SynStateCode {}

    assertNotEquals(codes.STATE_SUCCEED, codes.STATE_MERGE_PENDING)
    assertNotEquals(codes.STATE_FAIL, codes.STATE_MERGE_PENDING)
    assertNotEquals(codes.STATE_CANCEL, codes.STATE_MERGE_PENDING)
  }
}
