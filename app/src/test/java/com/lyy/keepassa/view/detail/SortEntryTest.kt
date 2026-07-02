/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression for https://github.com/AriaLyy/KeepassA/issues/117
 *
 * Symptom: entries named "1a", "1b", "1c" all sort under the same key,
 * so CHAR_ASC does not order them alphabetically.
 *
 * Root cause: the old sort key was `PinyinUtil.getFirstSpellChar(title) : Char?`,
 * which returns on the FIRST character (e.g. '1' for all of "1a/1b/1c"),
 * collapsing them to the same sort key with no tie-breaker.
 *
 * Rule confirmed with the user:
 *   - ASCII-led titles (digit / letter / symbol) come first, sorted by lowercased title.
 *   - Chinese-led titles come after, sorted by pinyin.
 */
class SortEntryTest {

  @Test
  fun `ascii-suffixed titles get distinct sort keys`() {
    val keys = listOf("1a", "1b", "1c").map(::sortKeyForTitle)
    println("keys = $keys")
    assertEquals("expected 3 distinct keys for 1a/1b/1c", 3, keys.toSet().size)
  }

  @Test
  fun `ascii prefix does not mask the rest`() {
    assertNotEquals(
      "'1a' and '1b' must differ in sort key",
      sortKeyForTitle("1a"),
      sortKeyForTitle("1b"),
    )
  }

  @Test
  fun `CHAR_ASC ordering of 1a 1b 1c is alphabetical`() {
    val input = listOf("1c", "1a", "1b", "2a", "10a")
    val sorted = input.sortedBy(::sortKeyForTitle)
    assertEquals(
      "asc sort must produce alphabetical order within the same digit prefix",
      listOf("10a", "1a", "1b", "1c", "2a"),
      sorted,
    )
  }

  @Test
  fun `ascii-led titles come before chinese-led titles`() {
    val asciiKeys = listOf("yellow", "Apple", "1a", "_note").map(::sortKeyForTitle)
    val chineseKeys = listOf("苹果", "香蕉", "中文").map(::sortKeyForTitle)
    asciiKeys.zip(chineseKeys).forEach { (a, c) ->
      assertTrue(
        "ASCII key '$a' must sort before Chinese key '$c'",
        a < c,
      )
    }
  }

  @Test
  fun `mixed list orders ascii first then chinese`() {
    val input = listOf("香蕉", "yellow", "苹果", "Apple", "1a", "中文")
    val sorted = input.sortedBy(::sortKeyForTitle)
    // Pinyin: 苹果=pg, 香蕉=xj, 中文=zw → Chinese bucket sorts pg<xj<zw
    assertEquals(
      "expected ASCII bucket first (sorted alphabetically), then Chinese bucket (by pinyin)",
      listOf("1a", "Apple", "yellow", "苹果", "香蕉", "中文"),
      sorted,
    )
  }
}
