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

    contexts[packageName] = AutofillBrowserAuthContext(
      packageName = packageName,
      domain = domain?.trim()?.takeIf { it.isNotEmpty() },
      metadata = metadata?.takeIf { it.autoFillIds.isNotEmpty() },
      fallbackId = fallbackId,
      fallbackRole = fallbackRole,
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
