package com.lyy.keepassa.service.feat

import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwIconCustom
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomIconSavePolicyTest {
  @Test
  fun zeroAndEmptyIconsAreNotSerializable() {
    assertFalse(CustomIconSavePolicy.isSerializable(PwIconCustom.ZERO))
    assertFalse(CustomIconSavePolicy.isSerializable(PwIconCustom(UUID.randomUUID(), byteArrayOf())))
    assertFalse(CustomIconSavePolicy.isSerializable(PwIconCustom(PwDatabaseV4.UUID_ZERO, byteArrayOf(1))))
  }

  @Test
  fun iconWithUuidAndImageDataIsSerializable() {
    assertTrue(CustomIconSavePolicy.isSerializable(PwIconCustom(UUID.randomUUID(), byteArrayOf(1, 2, 3))))
  }
}
