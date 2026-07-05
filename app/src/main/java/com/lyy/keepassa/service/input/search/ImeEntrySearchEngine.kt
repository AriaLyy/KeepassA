package com.lyy.keepassa.service.input.search

import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.getRealUserName

internal object ImeEntrySearchEngine {
  private const val MAX_RESULTS = 10

  private val blockedFieldNames = setOf(
    "password",
    "pwd",
    "pass",
    "otp",
    "totp",
    "totp seed",
    "timeotp secret base32",
    "timeotp secret base64",
    "timeotp secret hex",
    "hmacotp secret base32",
    "hmacotp secret base64",
    "hmacotp secret hex"
  )

  data class Candidate<T>(
    val value: T,
    val title: String,
    val username: String = "",
    val url: String = "",
    val notes: String = "",
    val customFields: List<String> = emptyList()
  )

  fun search(query: String): List<PwEntry> {
    val pm = BaseApp.KDB?.pm ?: return emptyList()
    val candidates = pm.entries.values.mapNotNull { entry ->
      val pwEntry = entry as? PwEntry ?: return@mapNotNull null
      Candidate(
        value = pwEntry,
        title = pwEntry.title.orEmpty(),
        username = pwEntry.getRealUserName(),
        url = pwEntry.url.orEmpty(),
        notes = when (pwEntry) {
          is PwEntryV4 -> pwEntry.notes.orEmpty()
          else -> ""
        },
        customFields = allowedCustomFieldValues(pwEntry)
      )
    }
    return searchCandidates(query, candidates)
  }

  fun <T> searchCandidates(
    query: String,
    candidates: List<Candidate<T>>
  ): List<T> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    return candidates.mapIndexedNotNull { index, candidate ->
      val rank = rank(candidate, normalizedQuery) ?: return@mapIndexedNotNull null
      RankedCandidate(index, rank, candidate.value)
    }
      .sortedWith(compareBy<RankedCandidate<T>> { it.rank }.thenBy { it.index })
      .take(MAX_RESULTS)
      .map { it.value }
  }

  fun isAllowedCustomField(
    key: String,
    protected: Boolean
  ): Boolean {
    if (protected) return false

    val normalized = key.trim().lowercase()
    if (normalized.isEmpty()) return false
    if (blockedFieldNames.contains(normalized)) return false
    if (normalized.contains("password")) return false
    if (normalized.contains("totp")) return false
    if (normalized.contains("otp")) return false

    return true
  }

  private fun <T> rank(
    candidate: Candidate<T>,
    query: String
  ): Int? {
    val title = candidate.title
    return when {
      title.equals(query, ignoreCase = true) -> 0
      title.startsWith(query, ignoreCase = true) -> 1
      title.contains(query, ignoreCase = true) -> 2
      candidate.url.contains(query, ignoreCase = true) -> 3
      candidate.username.contains(query, ignoreCase = true) -> 4
      candidate.notes.contains(query, ignoreCase = true) -> 5
      candidate.customFields.any { it.contains(query, ignoreCase = true) } -> 5
      else -> null
    }
  }

  private fun allowedCustomFieldValues(entry: PwEntry): List<String> {
    if (entry !is PwEntryV4) return emptyList()
    return entry.strings.mapNotNull { item ->
      val key = item.key ?: return@mapNotNull null
      val value = item.value ?: return@mapNotNull null
      if (!isAllowedCustomField(key, value.isProtected)) return@mapNotNull null
      value.toString()
    }
  }

  private data class RankedCandidate<T>(
    val index: Int,
    val rank: Int,
    val value: T
  )
}
