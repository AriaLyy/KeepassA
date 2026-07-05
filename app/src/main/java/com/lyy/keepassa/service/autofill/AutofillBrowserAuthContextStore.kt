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
import com.lyy.keepassa.util.CommonKVStorage
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
    nowMs: Long = System.currentTimeMillis(),
    persistentDomainStorage: AutofillBrowserPersistentDomainStorage =
      CommonKvAutofillBrowserPersistentDomainStorage
  ) {
    if (!strategy.isBrowser) {
      return
    }

    val existing = contexts[packageName]
    val previous = existing?.takeIf { nowMs - it.createdAtMs <= TTL_MS }
    if (existing != null && previous == null) {
      contexts.remove(packageName, existing)
    }
    val normalizedDomain = AutofillBrowserUrlPolicy.normalizeDomain(domain)
    if (strategy.persistDomainForSingleFieldFallback && normalizedDomain != null) {
      persistentDomainStorage.save(packageName, normalizedDomain, nowMs)
    }
    val normalizedMetadata = metadata?.takeIf { it.autoFillIds.isNotEmpty() }
    val domainChanged = normalizedDomain != null &&
      previous?.domain != null &&
      !normalizedDomain.equals(previous.domain, ignoreCase = true)
    val canCarryPreviousFieldContext = previous != null && !domainChanged
    val canCarryPreviousDomainForCurrentFallback = fallbackId != null &&
      previous != null &&
      !domainChanged &&
      strategy.reuseStoredDomainForSingleFieldFallback

    val next = AutofillBrowserAuthContext(
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
    contexts[packageName] = next
  }

  fun find(
    packageName: String,
    nowMs: Long = System.currentTimeMillis(),
    persistentDomainStorage: AutofillBrowserPersistentDomainStorage =
      CommonKvAutofillBrowserPersistentDomainStorage
  ): AutofillBrowserAuthContext? {
    val context = contexts[packageName]
    if (context == null) {
      return restorePersistentDomain(packageName, nowMs, persistentDomainStorage)
    }
    if (nowMs - context.createdAtMs > TTL_MS) {
      contexts.remove(packageName, context)
      return restorePersistentDomain(packageName, nowMs, persistentDomainStorage)
    }
    return context
  }

  fun clear(packageName: String) {
    contexts.remove(packageName)
  }

  fun clear() {
    contexts.clear()
  }

  private fun restorePersistentDomain(
    packageName: String,
    nowMs: Long,
    persistentDomainStorage: AutofillBrowserPersistentDomainStorage
  ): AutofillBrowserAuthContext? {
    val strategy = BrowserAutofillStrategyRegistry.forPackage(packageName)
    if (!strategy.isBrowser || !strategy.persistDomainForSingleFieldFallback) {
      return null
    }
    val persisted = persistentDomainStorage.find(packageName) ?: return null
    if (nowMs - persisted.createdAtMs > TTL_MS) {
      persistentDomainStorage.clear(packageName)
      return null
    }
    val domain = AutofillBrowserUrlPolicy.normalizeDomain(persisted.domain) ?: return null
    return AutofillBrowserAuthContext(
      packageName = packageName,
      domain = domain,
      metadata = null,
      fallbackId = null,
      fallbackRole = null,
      createdAtMs = persisted.createdAtMs
    ).also { contexts[packageName] = it }
  }
}

internal data class AutofillBrowserPersistentDomain(
  val domain: String,
  val createdAtMs: Long
)

/**
 * 短期保存浏览器域名,用于处理个别浏览器在 AutofillService 重建后只下发单字段
 * 虚拟 id 的场景。这里只保存域名和时间戳,不保存字段 id、账号、密码或完整 URL。
 */
internal interface AutofillBrowserPersistentDomainStorage {
  fun save(packageName: String, domain: String, nowMs: Long)
  fun find(packageName: String): AutofillBrowserPersistentDomain?
  fun clear(packageName: String)
  fun clear()
}

internal object CommonKvAutofillBrowserPersistentDomainStorage :
  AutofillBrowserPersistentDomainStorage {

  private const val KEY_PACKAGES = "autofill_browser_domain_packages"
  private const val KEY_PREFIX_DOMAIN = "autofill_browser_domain:"
  private const val KEY_PREFIX_CREATED_AT = "autofill_browser_domain_created_at:"

  override fun save(packageName: String, domain: String, nowMs: Long) {
    runCatching {
      CommonKVStorage.put(KEY_PREFIX_DOMAIN + packageName, domain)
      CommonKVStorage.put(KEY_PREFIX_CREATED_AT + packageName, nowMs)
      val packages = CommonKVStorage.getStringSet(KEY_PACKAGES).toMutableSet()
      packages.add(packageName)
      CommonKVStorage.put(KEY_PACKAGES, packages)
    }
  }

  override fun find(packageName: String): AutofillBrowserPersistentDomain? {
    return runCatching {
      val domain = CommonKVStorage.getString(KEY_PREFIX_DOMAIN + packageName)
        .takeIf { it.isNotBlank() }
        ?: return null
      val createdAtMs = CommonKVStorage.getLong(KEY_PREFIX_CREATED_AT + packageName, -1L)
        .takeIf { it >= 0L }
        ?: return null
      AutofillBrowserPersistentDomain(domain, createdAtMs)
    }.getOrNull()
  }

  override fun clear(packageName: String) {
    runCatching {
      CommonKVStorage.remove(KEY_PREFIX_DOMAIN + packageName)
      CommonKVStorage.remove(KEY_PREFIX_CREATED_AT + packageName)
      val packages = CommonKVStorage.getStringSet(KEY_PACKAGES).toMutableSet()
      if (packages.remove(packageName)) {
        CommonKVStorage.put(KEY_PACKAGES, packages)
      }
    }
  }

  override fun clear() {
    runCatching {
      val packages = CommonKVStorage.getStringSet(KEY_PACKAGES)
      packages.forEach { packageName ->
        CommonKVStorage.remove(KEY_PREFIX_DOMAIN + packageName)
        CommonKVStorage.remove(KEY_PREFIX_CREATED_AT + packageName)
      }
      CommonKVStorage.remove(KEY_PACKAGES)
    }
  }
}
