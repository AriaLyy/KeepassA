package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillTextValuePolicyTest {

  @Test
  fun usernameRoleUsesUsername() {
    assertEquals(
      "alice",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.USERNAME,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test
  fun passwordRoleUsesPassword() {
    assertEquals(
      "secret",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.PASSWORD,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test
  fun totpRoleUsesTotpWhenPresent() {
    assertEquals(
      "123456",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.TOTP,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test
  fun totpRoleReturnsNullWhenEntryHasNoTotp() {
    assertNull(
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.TOTP,
        username = "alice",
        password = "secret",
        totp = null
      )
    )
  }
}
