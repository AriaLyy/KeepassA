package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpAutofillSourceIntegrationTest {

  @Test
  fun metadataStoresDedicatedTotpRole() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt")
      .readText()

    assertTrue(source.contains("var fieldRole: AutofillFieldRole"))
    assertTrue(source.contains("AutofillFieldRole.TOTP"))
    assertTrue(source.contains("val isTotp: Boolean"))
    assertTrue(source.contains("AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP"))
  }
}
