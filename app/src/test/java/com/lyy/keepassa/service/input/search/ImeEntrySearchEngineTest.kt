package com.lyy.keepassa.service.input.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeEntrySearchEngineTest {

  @Test fun searchCandidates_ranksTitleBeforeUrlBeforeUsernameBeforeNotes() {
    val candidates = listOf(
      candidate("notes", title = "Alpha", notes = "github"),
      candidate("username", title = "Alpha", username = "github-user"),
      candidate("url", title = "Alpha", url = "https://github.com/login"),
      candidate("titleContains", title = "My GitHub"),
      candidate("titlePrefix", title = "GitHub Work"),
      candidate("titleExact", title = "github")
    )

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(
      listOf("titleExact", "titlePrefix", "titleContains", "url", "username", "notes"),
      result
    )
  }

  @Test fun searchCandidates_preservesOriginalOrderForSameRank() {
    val candidates = listOf(
      candidate("first", title = "GitHub one"),
      candidate("second", title = "GitHub two")
    )

    assertEquals(
      listOf("first", "second"),
      ImeEntrySearchEngine.searchCandidates("github", candidates)
    )
  }

  @Test fun searchCandidates_limitsToTenResults() {
    val candidates = (0 until 12).map { index ->
      candidate("item-$index", title = "GitHub $index")
    }

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(10, result.size)
    assertEquals("item-0", result.first())
    assertEquals("item-9", result.last())
  }

  @Test fun allowedField_excludesPasswordProtectedAndTotpFields() {
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("Password", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("TOTP Seed", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("otp", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("Custom", protected = true))
    assertTrue(ImeEntrySearchEngine.isAllowedCustomField("CustomerId", protected = false))
  }

  @Test fun searchCandidates_matchesAllowedCustomFieldsLast() {
    val candidates = listOf(
      candidate("custom", title = "Alpha", customFields = listOf("github")),
      candidate("username", title = "Alpha", username = "github")
    )

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(listOf("username", "custom"), result)
  }

  private fun candidate(
    value: String,
    title: String,
    username: String = "",
    url: String = "",
    notes: String = "",
    customFields: List<String> = emptyList()
  ) = ImeEntrySearchEngine.Candidate(
    value = value,
    title = title,
    username = username,
    url = url,
    notes = notes,
    customFields = customFields
  )
}
