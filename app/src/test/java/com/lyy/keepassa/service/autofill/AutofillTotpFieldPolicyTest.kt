package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillTotpFieldPolicyTest {

  @Test
  fun recognizesW3cOneTimeCode() {
    assertTrue(AutofillTotpFieldPolicy.isTotpToken("one-time-code"))
    assertTrue(
      AutofillTotpFieldPolicy.isTotpField(
        autofillHints = arrayOf("one-time-code"),
        idEntry = null,
        hint = null,
        htmlAttributes = null
      )
    )
  }

  @Test
  fun recognizesTotpFieldFromIdEntryAndHint() {
    assertTrue(
      AutofillTotpFieldPolicy.isTotpField(
        autofillHints = null,
        idEntry = "mfaCode",
        hint = null,
        htmlAttributes = null
      )
    )
    assertTrue(
      AutofillTotpFieldPolicy.isTotpField(
        autofillHints = null,
        idEntry = null,
        hint = "两步验证",
        htmlAttributes = null
      )
    )
  }

  @Test
  fun recognizesCommonTotpTokens() {
    listOf(
      "otp",
      "totp",
      "2fa",
      "mfa",
      "verification-code",
      "auth-code",
      "authenticator",
      "two_factor_code",
      "mfaCode"
    ).forEach { token ->
      assertTrue("$token should be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }

  @Test
  fun recognizesChineseTotpPrompts() {
    listOf("验证码", "动态码", "一次性密码", "二次验证", "两步验证").forEach { token ->
      assertTrue("$token should be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }

  @Test
  fun genericCodeAloneIsNotTotp() {
    listOf("code", "promo_code", "invite-code", "postal-code", "recovery code").forEach { token ->
      assertFalse("$token should not be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }
}
