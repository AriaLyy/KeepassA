/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import java.net.URI

internal object CredentialLookupTargetMapper {

  fun from(
    packageName: String?,
    origin: String?
  ): CredentialLookupTarget? {
    val normalizedPackage = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return CredentialLookupTarget(
      packageName = normalizedPackage,
      domain = normalizeDomain(origin)
    )
  }

  private fun normalizeDomain(origin: String?): String? {
    val raw = origin?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (raw.startsWith("android:", ignoreCase = true)) {
      return null
    }
    return runCatching {
      URI(raw).host?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    }.getOrNull()
  }
}
