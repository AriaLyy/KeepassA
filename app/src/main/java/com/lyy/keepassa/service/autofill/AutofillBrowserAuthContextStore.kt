/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import android.view.autofill.AutofillId
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadataCollection
import java.util.concurrent.ConcurrentHashMap

internal data class AutofillBrowserAuthContext(
  val packageName: String,
  val domain: String?,
  val metadata: AutoFillFieldMetadataCollection?,
  val fallbackId: AutofillId?,
  val fallbackRole: BrowserFormFieldRole?,
  val createdAtMs: Long
)

internal object AutofillBrowserAuthContextStore {
  const val TTL_MS = 5 * 60 * 1000L

  private val contexts = ConcurrentHashMap<String, AutofillBrowserAuthContext>()

  fun remember(
    packageName: String,
    strategy: BrowserAutofillStrategy,
    domain: String?,
    metadata: AutoFillFieldMetadataCollection?,
    fallbackId: AutofillId?,
    fallbackRole: BrowserFormFieldRole?,
    nowMs: Long = System.currentTimeMillis()
  ) {
    if (!strategy.isBrowser) {
      return
    }

    val existing = contexts[packageName]
    val previous = existing?.takeIf { nowMs - it.createdAtMs <= TTL_MS }
    if (existing != null && previous == null) {
      contexts.remove(packageName, existing)
    }
    val normalizedDomain = domain?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedMetadata = metadata?.takeIf { it.autoFillIds.isNotEmpty() }
    val domainChanged = normalizedDomain != null &&
      previous?.domain != null &&
      !normalizedDomain.equals(previous.domain, ignoreCase = true)
    val canCarryPreviousFieldContext = previous != null && !domainChanged
    val canCarryPreviousDomainForCurrentFallback = fallbackId != null &&
      previous != null &&
      !domainChanged &&
      UcBrowserAutofillCompatibility.canReuseStoredDomainForCurrentFallback(strategy)

    contexts[packageName] = AutofillBrowserAuthContext(
      packageName = packageName,
      domain = normalizedDomain ?: if (
        (fallbackId == null && canCarryPreviousFieldContext) ||
        canCarryPreviousDomainForCurrentFallback
      ) {
        previous?.domain
      } else {
        null
      },
      metadata = normalizedMetadata ?: if (fallbackId == null && canCarryPreviousFieldContext) {
        previous?.metadata
      } else {
        null
      },
      fallbackId = fallbackId ?: if (normalizedMetadata == null && canCarryPreviousFieldContext) {
        previous?.fallbackId
      } else {
        null
      },
      fallbackRole = fallbackRole ?: if (fallbackId == null && normalizedMetadata == null && canCarryPreviousFieldContext) {
        previous?.fallbackRole
      } else {
        null
      },
      createdAtMs = nowMs
    )
  }

  fun find(
    packageName: String,
    nowMs: Long = System.currentTimeMillis()
  ): AutofillBrowserAuthContext? {
    val context = contexts[packageName] ?: return null
    if (nowMs - context.createdAtMs > TTL_MS) {
      contexts.remove(packageName, context)
      return null
    }
    return context
  }

  fun clear(packageName: String) {
    contexts.remove(packageName)
  }

  fun clear() {
    contexts.clear()
  }
}
