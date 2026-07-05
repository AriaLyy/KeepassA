package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.UUID

class CredentialProviderPendingIntentsTest {

  @Test fun getPasswordExtrasIncludeLookupMetadataWithoutSecretValues() {
    val entryId = UUID.fromString("00000000-0000-0000-0000-000000000123")
    val target = CredentialLookupTarget(
      packageName = "com.example.app",
      domain = "example.com"
    )

    val extras = CredentialProviderPendingIntents.getPasswordExtras(
      entryId = entryId,
      target = target
    )

    assertEquals(entryId.toString(), extras[CredentialProviderPendingIntents.EXTRA_ENTRY_ID])
    assertEquals("com.example.app", extras[CredentialProviderPendingIntents.EXTRA_PACKAGE_NAME])
    assertEquals("example.com", extras[CredentialProviderPendingIntents.EXTRA_DOMAIN])
    assertFalse(extras.containsKey("password"))
    assertFalse(extras.containsKey("pass"))
  }
}
