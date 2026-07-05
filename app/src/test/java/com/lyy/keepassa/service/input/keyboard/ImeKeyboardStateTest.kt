package com.lyy.keepassa.service.input.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeKeyboardStateTest {

  @Test fun defaultState_isOrdinaryLowercaseAlphabet() {
    val state = ImeKeyboardState()

    assertFalse(state.isSearchMode)
    assertEquals(ImeKeyboardPage.ALPHABET, state.page)
    assertEquals(ImeShiftState.LOWERCASE, state.shiftState)
    assertEquals("", state.searchQuery)
  }

  @Test fun searchQuery_isOnlyChangedInSearchMode() {
    val state = ImeKeyboardState()

    assertFalse(state.appendSearchText("g"))
    state.enterSearchMode()
    assertTrue(state.appendSearchText("g"))
    assertTrue(state.appendSearchText("it"))

    assertEquals("git", state.searchQuery)
  }

  @Test fun clearAndExitSearchMode_clearsQuery() {
    val state = ImeKeyboardState()
    state.enterSearchMode()
    state.appendSearchText("github")

    state.exitSearchMode()

    assertFalse(state.isSearchMode)
    assertEquals("", state.searchQuery)
  }

  @Test fun backspaceInSearchMode_removesLastChar() {
    val state = ImeKeyboardState()
    state.enterSearchMode()
    state.appendSearchText("abc")

    assertTrue(state.backspaceSearchQuery())

    assertEquals("ab", state.searchQuery)
  }

  @Test fun singleShift_uppercasesOneCharacterThenResets() {
    val state = ImeKeyboardState()

    state.tapShift(nowMillis = 1_000)
    val first = state.applyShiftTo("a")
    val second = state.applyShiftTo("b")

    assertEquals("A", first)
    assertEquals("b", second)
    assertEquals(ImeShiftState.LOWERCASE, state.shiftState)
  }

  @Test fun doubleShift_locksUppercase() {
    val state = ImeKeyboardState()

    state.tapShift(nowMillis = 1_000)
    state.tapShift(nowMillis = 1_250)

    assertEquals(ImeShiftState.UPPERCASE_LOCKED, state.shiftState)
    assertEquals("A", state.applyShiftTo("a"))
    assertEquals("B", state.applyShiftTo("b"))
  }

  @Test fun longPressShift_locksUppercase() {
    val state = ImeKeyboardState()

    state.longPressShift()

    assertEquals(ImeShiftState.UPPERCASE_LOCKED, state.shiftState)
  }

  @Test fun switchPage_togglesAlphabetAndSymbols() {
    val state = ImeKeyboardState()

    state.switchToSymbols()
    assertEquals(ImeKeyboardPage.SYMBOLS, state.page)

    state.switchToAlphabet()
    assertEquals(ImeKeyboardPage.ALPHABET, state.page)
  }
}
