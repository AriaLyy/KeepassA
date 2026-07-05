package com.lyy.keepassa.service.input.keyboard

enum class ImeKeyboardPage {
  ALPHABET,
  SYMBOLS
}

enum class ImeShiftState {
  LOWERCASE,
  UPPERCASE_NEXT,
  UPPERCASE_LOCKED
}

class ImeKeyboardState {
  companion object {
    const val SHIFT_DOUBLE_TAP_WINDOW_MS = 400L
  }

  var isSearchMode: Boolean = false
    private set

  var page: ImeKeyboardPage = ImeKeyboardPage.ALPHABET
    private set

  var shiftState: ImeShiftState = ImeShiftState.LOWERCASE
    private set

  var searchQuery: String = ""
    private set

  private var lastShiftTapMillis: Long? = null

  fun enterSearchMode() {
    isSearchMode = true
    searchQuery = ""
  }

  fun exitSearchMode() {
    isSearchMode = false
    searchQuery = ""
  }

  fun clearSearchQuery() {
    searchQuery = ""
  }

  fun appendSearchText(text: String): Boolean {
    if (!isSearchMode) return false
    searchQuery += text
    return true
  }

  fun backspaceSearchQuery(): Boolean {
    if (!isSearchMode || searchQuery.isEmpty()) return false
    searchQuery = searchQuery.dropLast(1)
    return true
  }

  fun switchToSymbols() {
    page = ImeKeyboardPage.SYMBOLS
  }

  fun switchToAlphabet() {
    page = ImeKeyboardPage.ALPHABET
  }

  fun tapShift(nowMillis: Long) {
    val lastTap = lastShiftTapMillis
    shiftState = if (lastTap != null && nowMillis - lastTap <= SHIFT_DOUBLE_TAP_WINDOW_MS) {
      ImeShiftState.UPPERCASE_LOCKED
    } else {
      when (shiftState) {
        ImeShiftState.LOWERCASE -> ImeShiftState.UPPERCASE_NEXT
        ImeShiftState.UPPERCASE_NEXT -> ImeShiftState.UPPERCASE_LOCKED
        ImeShiftState.UPPERCASE_LOCKED -> ImeShiftState.LOWERCASE
      }
    }
    lastShiftTapMillis = nowMillis
  }

  fun longPressShift() {
    shiftState = ImeShiftState.UPPERCASE_LOCKED
  }

  fun applyShiftTo(text: String): String {
    val result = when (shiftState) {
      ImeShiftState.LOWERCASE -> text
      ImeShiftState.UPPERCASE_NEXT,
      ImeShiftState.UPPERCASE_LOCKED -> text.uppercase()
    }
    if (shiftState == ImeShiftState.UPPERCASE_NEXT) {
      shiftState = ImeShiftState.LOWERCASE
    }
    return result
  }
}
