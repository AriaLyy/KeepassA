/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepassAutofillServiceStatusTest {

  @Test fun managerEnabledSignalMeansKeepassAutofillIsEnabled() {
    assertTrue(
      KeepassAutofillServiceStatus.isEnabledFromSignals(
        isAutofillSupported = true,
        hasEnabledAutofillServices = true,
        managerServicePackageName = null,
        secureAutofillService = null,
        appPackageName = "com.lyy.keepassa",
        serviceClassName = "com.lyy.keepassa.service.autofill.AutoFillService"
      )
    )
  }

  @Test fun secureSettingMatchingKeepassServiceRecoversManagerFalseNegative() {
    assertTrue(
      KeepassAutofillServiceStatus.isEnabledFromSignals(
        isAutofillSupported = true,
        hasEnabledAutofillServices = false,
        managerServicePackageName = null,
        secureAutofillService =
        "com.lyy.keepassa/com.lyy.keepassa.service.autofill.AutoFillService",
        appPackageName = "com.lyy.keepassa",
        serviceClassName = "com.lyy.keepassa.service.autofill.AutoFillService"
      )
    )
  }

  @Test fun managerComponentPackageMatchingKeepassRecoversManagerFalseNegative() {
    assertTrue(
      KeepassAutofillServiceStatus.isEnabledFromSignals(
        isAutofillSupported = true,
        hasEnabledAutofillServices = false,
        managerServicePackageName = "com.lyy.keepassa",
        secureAutofillService = null,
        appPackageName = "com.lyy.keepassa",
        serviceClassName = "com.lyy.keepassa.service.autofill.AutoFillService"
      )
    )
  }

  @Test fun differentSecureAutofillServiceDoesNotEnableKeepassPrompt() {
    assertFalse(
      KeepassAutofillServiceStatus.isEnabledFromSignals(
        isAutofillSupported = true,
        hasEnabledAutofillServices = false,
        managerServicePackageName = null,
        secureAutofillService =
        "com.example.password/com.example.password.AutofillService",
        appPackageName = "com.lyy.keepassa",
        serviceClassName = "com.lyy.keepassa.service.autofill.AutoFillService"
      )
    )
  }

  @Test fun unsupportedAutofillNeverCountsAsEnabled() {
    assertFalse(
      KeepassAutofillServiceStatus.isEnabledFromSignals(
        isAutofillSupported = false,
        hasEnabledAutofillServices = true,
        managerServicePackageName = "com.lyy.keepassa",
        secureAutofillService =
        "com.lyy.keepassa/com.lyy.keepassa.service.autofill.AutoFillService",
        appPackageName = "com.lyy.keepassa",
        serviceClassName = "com.lyy.keepassa.service.autofill.AutoFillService"
      )
    )
  }
}
