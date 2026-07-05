/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillBrowserUrlPolicyTest {

  @Test fun extractsHostFromFullHttpsUrl() {
    assertEquals(
      "login.example.com",
      AutofillBrowserUrlPolicy.extractDomainFromAddressValue("https://login.example.com/account")
    )
  }

  @Test fun extractsHostFromBareUrlWithPath() {
    assertEquals(
      "carpt.net",
      AutofillBrowserUrlPolicy.extractDomainFromAddressValue("carpt.net/login")
    )
  }

  @Test fun ignoresSearchTextWithoutHost() {
    assertNull(AutofillBrowserUrlPolicy.extractDomainFromAddressValue("keepass password manager"))
  }

  @Test fun ignoresBrowserPseudoDomain() {
    assertNull(AutofillBrowserUrlPolicy.normalizeDomain("newtab"))
  }
}
