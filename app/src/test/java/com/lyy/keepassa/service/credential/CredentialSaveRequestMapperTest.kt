package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialSaveRequestMapperTest {

  @Test fun mapsValidPasswordSaveRequest() {
    val draft = CredentialSaveRequestMapper.from(
      packageName = " com.example.app ",
      origin = "https://example.com/login",
      username = " alice ",
      password = "secret"
    )

    assertEquals("com.example.app", draft?.packageName)
    assertEquals("example.com", draft?.domain)
    assertEquals("alice", draft?.username)
    assertEquals("secret", draft?.password)
  }

  @Test fun returnsNullWhenPackageIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "",
        origin = "https://example.com",
        username = "alice",
        password = "secret"
      )
    )
  }

  @Test fun returnsNullWhenUsernameIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "com.example.app",
        origin = "https://example.com",
        username = " ",
        password = "secret"
      )
    )
  }

  @Test fun returnsNullWhenPasswordIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "com.example.app",
        origin = "https://example.com",
        username = "alice",
        password = ""
      )
    )
  }
}
