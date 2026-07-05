package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialPasswordResultMapperTest {

  @Test fun mapsNonBlankUsernameAndPassword() {
    val result = CredentialPasswordResultMapper.from(
      username = " alice ",
      password = "secret"
    )

    assertEquals("alice", result?.username)
    assertEquals("secret", result?.password)
  }

  @Test fun returnsNullForBlankUsername() {
    assertNull(
      CredentialPasswordResultMapper.from(
        username = " ",
        password = "secret"
      )
    )
  }

  @Test fun returnsNullForEmptyPassword() {
    assertNull(
      CredentialPasswordResultMapper.from(
        username = "alice",
        password = ""
      )
    )
  }
}
