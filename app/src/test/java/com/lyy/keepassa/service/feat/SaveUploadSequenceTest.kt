package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.SynStateCode
import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SaveUploadSequenceTest {
  private val stateCode = object : SynStateCode {}

  @Test
  fun run_doesNotUploadWhenDatabaseSaveFails() = runBlocking {
    var uploaded = false

    val response = SaveUploadSequence.run(
      save = { stateCode.STATE_SAVE_DB_FAIL },
      upload = {
        uploaded = true
        DbSyncResponse(stateCode.STATE_SUCCEED, "uploaded")
      }
    )

    assertFalse(uploaded)
    assertEquals(stateCode.STATE_SAVE_DB_FAIL, response.code)
  }
}
