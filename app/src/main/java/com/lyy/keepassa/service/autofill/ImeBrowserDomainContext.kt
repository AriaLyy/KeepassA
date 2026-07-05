package com.lyy.keepassa.service.autofill

internal object ImeBrowserDomainContext {
  const val TTL_MS = 60_000L

  enum class Source {
    WEB_DOMAIN,
    ADDRESS_BAR,
    HTML_VALUE
  }

  data class Snapshot(
    val browserPackage: String,
    val domain: String,
    val updatedAt: Long,
    val source: Source
  )

  private var clock: () -> Long = { System.currentTimeMillis() }
  private var snapshot: Snapshot? = null

  fun remember(
    browserPackage: String,
    domain: String,
    source: Source
  ) {
    val normalizedPackage = browserPackage.trim()
    val normalizedDomain = domain.trim()
    if (normalizedPackage.isEmpty() || normalizedDomain.isEmpty()) {
      clear()
      return
    }
    snapshot = Snapshot(
      browserPackage = normalizedPackage,
      domain = normalizedDomain,
      updatedAt = clock(),
      source = source
    )
  }

  fun resolve(browserPackage: String?): String? {
    val current = snapshot ?: return null
    if (browserPackage.isNullOrBlank()) return null
    if (!current.browserPackage.equals(browserPackage.trim(), ignoreCase = true)) return null
    if (clock() - current.updatedAt > TTL_MS) return null
    return current.domain
  }

  fun clear() {
    snapshot = null
  }

  fun setClockForTest(clock: () -> Long) {
    this.clock = clock
  }

  fun resetForTest() {
    clock = { System.currentTimeMillis() }
    snapshot = null
  }
}
