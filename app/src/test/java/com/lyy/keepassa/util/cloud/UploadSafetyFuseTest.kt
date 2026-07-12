package com.lyy.keepassa.util.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UploadSafetyFuseTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun trippedFuseSurvivesNewRepositoryInstanceUntilExplicitlyCleared() {
    val root = tempFolder.newFolder("fuses")
    val cloudPath = "https://dav.example.com/db.kdbx"

    UploadSafetyFuse(root).trip(cloudPath)

    assertTrue(UploadSafetyFuse(root).isTripped(cloudPath))
    UploadSafetyFuse(root).clear(cloudPath)
    assertFalse(UploadSafetyFuse(root).isTripped(cloudPath))
  }
}
