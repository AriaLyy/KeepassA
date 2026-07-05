package com.lyy.keepassa.service.input.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeKeyboardLayoutTest {

  @Test fun alphabetRows_matchQwertyDesign() {
    val rows = ImeKeyboardLayout.alphabetRows()

    assertEquals("qwertyuiop", rows[0].characters())
    assertEquals("asdfghjkl", rows[1].characters())
    assertEquals(
      "zxcvbnm",
      rows[2].filterIsInstance<ImeKeyAction.CommitText>().joinToString("") { it.text }
    )
  }

  @Test fun alphabetRows_includeRequiredFunctionKeys() {
    val flattened = ImeKeyboardLayout.alphabetRows().flatten()

    assertTrue(flattened.contains(ImeKeyAction.Shift))
    assertTrue(flattened.contains(ImeKeyAction.Backspace))
    assertTrue(flattened.contains(ImeKeyAction.SwitchToSymbols))
    assertTrue(flattened.contains(ImeKeyAction.Space))
    assertTrue(flattened.contains(ImeKeyAction.Enter))
  }

  @Test fun symbolRows_includeLoginAndSearchCharacters() {
    val chars = ImeKeyboardLayout.symbolRows()
      .flatten()
      .filterIsInstance<ImeKeyAction.CommitText>()
      .joinToString("") { it.text }

    listOf(
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
      "@", ".", "_", "-", "+", "/", ":", "?", "&", "=", "#", "%", "!", "*", "(", ")"
    ).forEach { expected ->
      assertTrue("missing $expected in $chars", chars.contains(expected))
    }
  }

  @Test fun symbolRows_includeRequiredFunctionKeys() {
    val flattened = ImeKeyboardLayout.symbolRows().flatten()

    assertTrue(flattened.contains(ImeKeyAction.SwitchToAlphabet))
    assertTrue(flattened.contains(ImeKeyAction.Backspace))
    assertTrue(flattened.contains(ImeKeyAction.Space))
    assertTrue(flattened.contains(ImeKeyAction.Enter))
  }

  private fun List<ImeKeyAction>.characters(): String =
    filterIsInstance<ImeKeyAction.CommitText>().joinToString("") { it.text }
}
