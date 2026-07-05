package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSaveFieldPolicyTest {

  @Test
  fun passwordFieldsCanBeSavedAsPassword() {
    assertTrue(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = true, isTotp = false))
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = true, isTotp = false))
  }

  @Test
  fun usernameFieldsCanBeSavedAsUsername() {
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = false, isTotp = false))
    assertTrue(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = false, isTotp = false))
  }

  @Test
  fun totpFieldsAreNeverSavedAsUsernameOrPassword() {
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = false, isTotp = true))
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = false, isTotp = true))
  }
}
