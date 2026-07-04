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

class AutofillFillRequestPolicyTest {

  @Test fun ownPackageRequest_mustCompleteWithNullResponse() {
    assertTrue(
      AutofillFillRequestPolicy.shouldCompleteWithNullResponse(
        targetPackageName = "com.lyy.keepassa",
        servicePackageName = "com.lyy.keepassa",
        canBackgroundStart = true
      )
    )
  }

  @Test fun backgroundStartDeniedRequest_mustCompleteWithNullResponse() {
    assertTrue(
      AutofillFillRequestPolicy.shouldCompleteWithNullResponse(
        targetPackageName = "com.microsoft.emmx",
        servicePackageName = "com.lyy.keepassa",
        canBackgroundStart = false
      )
    )
  }

  @Test fun normalThirdPartyRequest_shouldContinueProcessing() {
    assertFalse(
      AutofillFillRequestPolicy.shouldCompleteWithNullResponse(
        targetPackageName = "com.microsoft.emmx",
        servicePackageName = "com.lyy.keepassa",
        canBackgroundStart = true
      )
    )
  }
}
