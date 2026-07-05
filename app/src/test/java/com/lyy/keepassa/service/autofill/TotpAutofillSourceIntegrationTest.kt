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

  @Test
  fun structureParserKeepsTotpOnlyRequests() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt")
      .readText()

    assertTrue(source.contains("val totpFields = ArrayList<ViewNode>()"))
    assertTrue(source.contains("totpFields.clear()"))
    assertTrue(source.contains("passFields.isEmpty() && totpFields.isEmpty() && !isManual && !isW3c"))
    assertTrue(source.contains("private fun addTotpField("))
    assertTrue(source.contains("AutoFillFieldMetadata(viewNode, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP)"))
  }

  @Test
  fun w3cHintsExposeTotpDetectionAndDoNotTreatEveryAutocompleteAsPassword() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt")
      .readText()

    assertTrue(source.contains("fun isW3cTotpByHints(viewNode: ViewNode): Boolean"))
    assertTrue(source.contains("AutofillTotpFieldPolicy.isTotpField("))
    assertTrue(source.contains("name == \"autocomplete\" && PASSWORD_HINT_LIST.contains(value)"))
  }
}
