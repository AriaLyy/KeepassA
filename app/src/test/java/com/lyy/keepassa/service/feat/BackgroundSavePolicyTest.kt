package com.lyy.keepassa.service.feat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundSavePolicyTest {
  @Test fun cleanDatabaseDoesNotSaveOrUploadWhenAppEntersBackground() {
    assertFalse(BackgroundSavePolicy.shouldSave(hasDirtyGroups = false))
  }

  @Test fun dirtyDatabaseSavesAndUploadsWhenAppEntersBackground() {
    assertTrue(BackgroundSavePolicy.shouldSave(hasDirtyGroups = true))
  }
}
