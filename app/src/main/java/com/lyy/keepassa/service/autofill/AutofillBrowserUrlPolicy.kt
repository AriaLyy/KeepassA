/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.net.URI
import java.util.Locale

internal object AutofillBrowserUrlPolicy {

  fun normalizeDomain(value: CharSequence?): String? {
    val raw = value?.toString()?.trim()?.lowercase(Locale.ROOT)?.trim('.') ?: return null
    if (raw.isEmpty() || raw.any(Char::isWhitespace)) {
      return null
    }
    return raw.takeIf { it.contains('.') || isIpv4Address(it) }
  }

  fun extractDomainFromAddressValue(value: CharSequence?): String? {
    val raw = value?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (raw.any(Char::isWhitespace)) {
      return null
    }

    val candidate = if (raw.contains("://")) raw else "https://$raw"
    val host = runCatching { URI(candidate).host }
      .getOrNull()
      ?.lowercase(Locale.ROOT)
      ?.trim('.')
      ?.takeIf { it.isNotEmpty() }
      ?: return null

    return normalizeDomain(host)
  }

  private fun isIpv4Address(value: String): Boolean {
    val parts = value.split('.')
    return parts.size == 4 && parts.all { part ->
      part.toIntOrNull()?.let { it in 0..255 } == true
    }
  }
}
