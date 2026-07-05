/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

internal object CredentialPasswordResultMapper {

  fun from(
    username: String?,
    password: String?
  ): CredentialPasswordResult? {
    val normalizedUsername = username?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val normalizedPassword = password?.takeIf { it.isNotEmpty() } ?: return null
    return CredentialPasswordResult(
      username = normalizedUsername,
      password = normalizedPassword
    )
  }
}
