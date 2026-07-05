package com.lyy.keepassa.service.input.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeSearchSessionTest {

  @Test fun debounceConstant_is250Milliseconds() {
    assertEquals(250L, ImeSearchSession.DEBOUNCE_MS)
  }

  @Test fun updateResults_selectsFirstResultByDefault() {
    val session = ImeSearchSession<String>()

    session.updateResults(listOf("a", "b"))

    assertEquals("a", session.selected)
    assertEquals(listOf("a", "b"), session.results)
    assertFalse(session.isEmptyStateVisible)
  }

  @Test fun updateResults_emptyShowsEmptyStateWhenQueryIsNotBlank() {
    val session = ImeSearchSession<String>()
    session.setQuery("github")

    session.updateResults(emptyList())

    assertNull(session.selected)
    assertTrue(session.isEmptyStateVisible)
  }

  @Test fun clearQuery_clearsResultsAndSelection() {
    val session = ImeSearchSession<String>()
    session.setQuery("github")
    session.updateResults(listOf("a"))

    session.clear()

    assertEquals("", session.query)
    assertTrue(session.results.isEmpty())
    assertNull(session.selected)
    assertFalse(session.isEmptyStateVisible)
  }

  @Test fun select_existingResult_updatesSelected() {
    val session = ImeSearchSession<String>()
    session.updateResults(listOf("a", "b"))

    assertTrue(session.select("b"))

    assertEquals("b", session.selected)
  }
}
