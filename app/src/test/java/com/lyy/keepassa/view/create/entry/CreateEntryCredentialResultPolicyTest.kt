package com.lyy.keepassa.view.create.entry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEntryCredentialResultPolicyTest {

  @Test fun returnsOkOnlyForCredentialResultModeAndSuccessfulSave() {
    assertTrue(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = true,
        saveSucceeded = true
      )
    )
  }

  @Test fun doesNotReturnOkForNormalCreateFlow() {
    assertFalse(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = false,
        saveSucceeded = true
      )
    )
  }

  @Test fun doesNotReturnOkWhenSaveFails() {
    assertFalse(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = true,
        saveSucceeded = false
      )
    )
  }
}
