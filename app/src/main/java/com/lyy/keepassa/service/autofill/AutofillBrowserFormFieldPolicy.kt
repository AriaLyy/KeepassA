/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

internal enum class BrowserFormFieldRole {
  USERNAME,
  PASSWORD
}

internal data class BrowserFormFieldCandidate(
  val index: Int,
  val top: Int,
  val isFocused: Boolean,
  val isPassword: Boolean,
  val isSearchOrUrl: Boolean
)

internal object AutofillBrowserFormFieldPolicy {

  fun inferCredentialRoles(
    candidates: List<BrowserFormFieldCandidate>
  ): Map<Int, BrowserFormFieldRole> {
    val fields = candidates
      .filterNot { it.isSearchOrUrl }
      .sortedWith(compareBy<BrowserFormFieldCandidate> { it.top }.thenBy { it.index })

    if (fields.size < 2) {
      return emptyMap()
    }

    val focusedField = fields.firstOrNull { it.isFocused }
    val passwordField = inferPasswordField(fields, focusedField) ?: return emptyMap()
    val usernameField = inferUsernameField(fields, focusedField, passwordField) ?: return emptyMap()
    if (usernameField.index == passwordField.index) {
      return emptyMap()
    }

    return mapOf(
      usernameField.index to BrowserFormFieldRole.USERNAME,
      passwordField.index to BrowserFormFieldRole.PASSWORD
    )
  }

  private fun inferPasswordField(
    fields: List<BrowserFormFieldCandidate>,
    focusedField: BrowserFormFieldCandidate?
  ): BrowserFormFieldCandidate? {
    if (focusedField?.isPassword == true) {
      return focusedField
    }
    if (focusedField != null) {
      fields.firstOrNull { it.isPassword && it.top >= focusedField.top }?.let {
        return it
      }
      fields.firstOrNull { it.top > focusedField.top }?.let {
        return it
      }
    }
    return fields.firstOrNull { it.isPassword } ?: fields.getOrNull(1)
  }

  private fun inferUsernameField(
    fields: List<BrowserFormFieldCandidate>,
    focusedField: BrowserFormFieldCandidate?,
    passwordField: BrowserFormFieldCandidate
  ): BrowserFormFieldCandidate? {
    if (focusedField != null && !focusedField.isPassword && focusedField.index != passwordField.index) {
      return focusedField
    }
    return fields
      .filter { it.index != passwordField.index && !it.isPassword }
      .lastOrNull { it.top <= passwordField.top }
      ?: fields.firstOrNull { it.index != passwordField.index }
  }
}
