package com.lyy.keepassa.service.autofill

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImeBrowserDomainContextTest {

  private var now = 10_000L

  @After fun tearDown() {
    ImeBrowserDomainContext.resetForTest()
  }

  @Test fun resolve_samePackageWithinTtl_returnsDomain() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    assertEquals("example.com", ImeBrowserDomainContext.resolve("com.android.chrome"))
  }

  @Test fun resolve_differentPackage_returnsNull() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.ADDRESS_BAR
    )

    assertNull(ImeBrowserDomainContext.resolve("com.vivaldi.browser"))
  }

  @Test fun resolve_expiredContext_returnsNull() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    now += ImeBrowserDomainContext.TTL_MS + 1

    assertNull(ImeBrowserDomainContext.resolve("com.android.chrome"))
  }

  @Test fun remember_blankDomain_clearsContext() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    assertNull(ImeBrowserDomainContext.resolve("com.android.chrome"))
  }
}
