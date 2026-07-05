package com.lyy.keepassa.service.input.search

internal class ImeSearchSession<T> {
  companion object {
    const val DEBOUNCE_MS = 250L
  }

  var query: String = ""
    private set

  var results: List<T> = emptyList()
    private set

  var selected: T? = null
    private set

  val isEmptyStateVisible: Boolean
    get() = query.trim().isNotEmpty() && results.isEmpty()

  fun setQuery(value: String) {
    query = value
  }

  fun appendToQuery(value: String) {
    query += value
  }

  fun backspaceQuery() {
    if (query.isNotEmpty()) {
      query = query.dropLast(1)
    }
  }

  fun updateResults(results: List<T>) {
    this.results = results
    selected = results.firstOrNull()
  }

  fun select(item: T): Boolean {
    if (!results.contains(item)) return false
    selected = item
    return true
  }

  fun clear() {
    query = ""
    results = emptyList()
    selected = null
  }
}
