package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialLookupTargetMapperTest {

  @Test fun mapsPackageNameAndNormalizesDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = " com.example.app ",
      origin = "https://Login.Example.com/path"
    )

    assertEquals("com.example.app", target?.packageName)
    assertEquals("login.example.com", target?.domain)
  }

  @Test fun mapsPackageNameWithoutDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = "com.example.app",
      origin = null
    )

    assertEquals("com.example.app", target?.packageName)
    assertNull(target?.domain)
  }

  @Test fun returnsNullForBlankPackageName() {
    val target = CredentialLookupTargetMapper.from(
      packageName = " ",
      origin = "https://example.com"
    )

    assertNull(target)
  }

  @Test fun stripsAndroidApkOriginIntoNoDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = "com.example.app",
      origin = "android:apk-key-hash:abc"
    )

    assertEquals("com.example.app", target?.packageName)
    assertNull(target?.domain)
  }
}
