/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSearchOrUrlFieldPolicyTest {

  @Test fun onlySearchOrUrlClassifiedFields_areIgnored() {
    val urlFieldId = mockk<AutofillId>()

    assertTrue(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        classifiedIds = setOf(urlFieldId),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }

  @Test fun classifiedWebCredentialField_isKept() {
    val urlFieldId = mockk<AutofillId>()
    val webFieldId = mockk<AutofillId>()

    assertFalse(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        classifiedIds = setOf(urlFieldId, webFieldId),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }

  @Test fun emptyClassifiedFields_areNotIgnored() {
    val urlFieldId = mockk<AutofillId>()

    assertFalse(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        classifiedIds = emptySet(),
        searchOrUrlIds = setOf(urlFieldId)
      )
    )
  }

  @Test fun emptySearchOrUrlIds_doesNotIgnore() {
    val webFieldId = mockk<AutofillId>()

    // 没有 URL/搜索栏信息时不应该错误丢弃已分类字段。
    assertFalse(
      AutofillSearchOrUrlFieldPolicy.shouldIgnoreClassifiedFields(
        classifiedIds = setOf(webFieldId),
        searchOrUrlIds = emptySet()
      )
    )
  }
}
