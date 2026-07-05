package com.lyy.keepassa.service.credential

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialUnlockIntentPolicyTest {

  @Test fun usesQuickUnlockOnlyWhenDatabaseIsOpenAndQuickUnlockIsAvailable() {
    assertTrue(
      CredentialUnlockIntentPolicy.shouldUseQuickUnlock(
        hasOpenDatabase = true,
        canOpenQuickUnlock = true
      )
    )
  }

  @Test fun usesFullUnlockWhenDatabaseIsNotOpen() {
    assertFalse(
      CredentialUnlockIntentPolicy.shouldUseQuickUnlock(
        hasOpenDatabase = false,
        canOpenQuickUnlock = true
      )
    )
  }

  @Test fun usesFullUnlockWhenQuickUnlockIsDisabled() {
    assertFalse(
      CredentialUnlockIntentPolicy.shouldUseQuickUnlock(
        hasOpenDatabase = true,
        canOpenQuickUnlock = false
      )
    )
  }
}
