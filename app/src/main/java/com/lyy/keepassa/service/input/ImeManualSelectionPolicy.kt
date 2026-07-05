package com.lyy.keepassa.service.input

internal class ImeManualSelectionPolicy<T> {
  private var packageName: String? = null

  var currentSelection: T? = null
    private set

  fun rememberManualSelection(
    packageName: String?,
    selection: T
  ) {
    this.packageName = packageName?.trim()?.takeIf { it.isNotEmpty() }
    currentSelection = selection
  }

  fun onStartInput(newPackageName: String?) {
    val currentPackage = packageName ?: return
    if (!currentPackage.equals(newPackageName?.trim(), ignoreCase = true)) {
      clear()
    }
  }

  fun hasManualSelectionFor(packageName: String?): Boolean {
    val currentPackage = this.packageName ?: return false
    return currentSelection != null && currentPackage.equals(packageName?.trim(), ignoreCase = true)
  }

  fun shouldUseAutomaticCandidates(packageName: String?): Boolean =
    !hasManualSelectionFor(packageName)

  fun onNewSearch() {
    clear()
  }

  fun clear() {
    packageName = null
    currentSelection = null
  }
}
