package com.lyy.keepassa.view.main

import android.content.SearchRecentSuggestionsProvider

class SearchSuggestionProvider : SearchRecentSuggestionsProvider() {
  init {
    setupSuggestions(AUTHORITY, MODE)
  }

  companion object {
    const val AUTHORITY = "com.example.MySuggestionProvider"
    const val MODE: Int = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES
  }
}
    