/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillBrowserFormFieldPolicyTest {

  @Test fun focusedUsername_usesNextLowerTextFieldAsPassword() {
    val roles = AutofillBrowserFormFieldPolicy.inferCredentialRoles(
      listOf(
        candidate(index = 0, top = 100, isFocused = true),
        candidate(index = 1, top = 180)
      )
    )

    assertEquals(BrowserFormFieldRole.USERNAME, roles[0])
    assertEquals(BrowserFormFieldRole.PASSWORD, roles[1])
  }

  @Test fun focusedPassword_usesNearestUpperTextFieldAsUsername() {
    val roles = AutofillBrowserFormFieldPolicy.inferCredentialRoles(
      listOf(
        candidate(index = 0, top = 100),
        candidate(index = 1, top = 180, isFocused = true, isPassword = true)
      )
    )

    assertEquals(BrowserFormFieldRole.USERNAME, roles[0])
    assertEquals(BrowserFormFieldRole.PASSWORD, roles[1])
  }

  @Test fun searchOrUrlField_isIgnoredWhenInferringCredentialRoles() {
    val roles = AutofillBrowserFormFieldPolicy.inferCredentialRoles(
      listOf(
        candidate(index = 0, top = 10, isFocused = true, isSearchOrUrl = true),
        candidate(index = 1, top = 100, isFocused = true),
        candidate(index = 2, top = 180)
      )
    )

    assertEquals(BrowserFormFieldRole.USERNAME, roles[1])
    assertEquals(BrowserFormFieldRole.PASSWORD, roles[2])
    assertTrue(roles[0] == null)
  }

  @Test fun singleAmbiguousField_doesNotInferCredentials() {
    val roles = AutofillBrowserFormFieldPolicy.inferCredentialRoles(
      listOf(candidate(index = 0, top = 100, isFocused = true))
    )

    assertTrue(roles.isEmpty())
  }

  private fun candidate(
    index: Int,
    top: Int,
    isFocused: Boolean = false,
    isPassword: Boolean = false,
    isSearchOrUrl: Boolean = false
  ) = BrowserFormFieldCandidate(
    index = index,
    top = top,
    isFocused = isFocused,
    isPassword = isPassword,
    isSearchOrUrl = isSearchOrUrl
  )
}
