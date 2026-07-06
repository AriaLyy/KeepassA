/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class StructureParserSourceTest {

  @Test fun browserNativeEditTextClassificationSkipsSearchOrUrlFields() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt").readText()

    assertTrue(
      "Browser native EditText classification must not treat address/search fields as username fields.",
      source.contains(
        "browserStrategy.shouldClassifyNativeEditTextVirtualNodes &&\n" +
          "        classIsEditText(className) &&\n" +
          "        !isLikelySearchOrUrlField(viewNode)"
      )
    )
  }

  @Test fun totpDisambiguationPrefersSpecificTokensOverGenericCode() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt").readText()

    assertTrue(
      "TOTP disambiguation must be invoked after browser fallback and before the empty-passField check.",
      source.contains("applyBrowserFallbackCredentialFields()\n    applyTotpDisambiguation()")
    )
    assertTrue(
      "TOTP disambiguation must be a no-op when there is only one candidate.",
      source.contains("if (totpFields.size <= 1) return")
    )
    assertTrue(
      "Specific TOTP tokens must include 两步验证 / 二次验证 / 动态码 / 动态密码 / 一次性密码.",
      source.contains("\"两步验证\", \"二次验证\", \"动态码\", \"动态密码\", \"一次性密码\"")
    )
    assertTrue(
      "Specific English TOTP tokens must include otp / totp / 2fa / mfa / authenticator / onetimecode.",
      source.contains("\"otp\", \"totp\", \"2fa\", \"mfa\", \"authenticator\", \"onetimecode\"")
    )
    assertTrue(
      "Generic TOTP candidates must be removed from autoFillFields so they are not filled.",
      source.contains("autoFillFields.removeField(autofillId, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP)")
    )
  }
}
