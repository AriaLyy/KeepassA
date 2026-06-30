/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 修复用户反馈 bug ④(同包名多候选时,选第二个却填充第一个;再点第二个时两个同时高亮)的回归测试。
 *
 * 旧实现的状态散在三处:
 *   - InputIMEService.curEntry
 *   - CandidatesAdapter.bindData 用的 SimpleItemEntity.isSelected
 *   - OnItemClickListener 匿名对象的 lastPosition
 * 三者通过 showEntryList / onItemClicked 间接同步,但 showEntryList 会无条件重置 curEntry=entries[0],
 * 导致用户在候选列表的选择被按钮点击覆盖。
 */
class CandidateSelectionTrackerTest {

  @Test fun show_multipleEntries_defaultsSelectedToFirst() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    assertEquals("A", tracker.current)
    assertEquals(listOf(true, false), tracker.selectedFlags())
  }

  @Test fun show_singleEntry_autoSelected() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("only"))
    assertEquals("only", tracker.current)
    assertEquals(listOf(true), tracker.selectedFlags())
  }

  @Test fun show_emptyList_clearsSelection() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    tracker.show(emptyList())
    assertNull(tracker.current)
    assertTrue(tracker.selectedFlags().isEmpty())
    assertTrue(tracker.isEmpty)
  }

  @Test fun click_changesCurrentAndFlags() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))

    val changed = tracker.click(1)

    assertTrue(changed)
    assertEquals("B", tracker.current)
    assertEquals(listOf(false, true), tracker.selectedFlags())
  }

  @Test fun click_sameIndex_isNoOp() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))

    val changed = tracker.click(0)

    assertFalse(changed)
    assertEquals("A", tracker.current)
  }

  @Test fun click_outOfBounds_isNoOp() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))

    assertFalse(tracker.click(-1))
    assertFalse(tracker.click(2))
    assertEquals("A", tracker.current)
  }

  /**
   * 用户反馈 bug ④ 主症状:show([A,B]) → click(1) 后,fill 应得 B,而非 A。
   */
  @Test fun fill_after_click_nonDefault_returns_clicked_entry() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    tracker.click(1)
    assertEquals("B", tracker.current)
  }

  /**
   * 用户反馈 bug ④ 次症状:用户在候选列表选了 B 后,按钮点击触发"再次 show",
   * 但旧实现会把 curEntry 重置为 A。用 resync 而不是 show,保留选择。
   */
  @Test fun resync_sameEntries_preservesSelection() {
    val tracker = CandidateSelectionTracker<String>()
    val first = listOf("A", "B")
    tracker.show(first)
    tracker.click(1)

    val preserved = tracker.resync(first)

    assertTrue(preserved)
    assertEquals("B", tracker.current)
    assertEquals(listOf(false, true), tracker.selectedFlags())
  }

  /**
   * 旧 bug:show 后 candidatesData[0].isSelected=true,但 OnItemClickListener.lastPosition
   * 残留为上一次的值;再 click 同一 position 时,candidatesData[0] 的选中态从未被取消。
   * tracker 把 selectedIndex 集中管理后,该症状自动消失。
   */
  @Test fun rebuild_then_click_keeps_only_one_selected() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    tracker.click(1)

    tracker.show(listOf("A", "B"))  // 模拟"重新搜索"
    tracker.click(1)                 // 用户再次点 B

    assertEquals(listOf(false, true), tracker.selectedFlags())
  }

  @Test fun resync_differentEntries_fallsBackToShow() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    tracker.click(1)

    val preserved = tracker.resync(listOf("A", "C"))

    assertFalse(preserved)
    assertEquals("A", tracker.current)
    assertEquals(listOf(true, false), tracker.selectedFlags())
  }

  @Test fun resync_afterEmptyShow_recoversSelection() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.resync(listOf("A", "B"))
    assertEquals("A", tracker.current)
  }

  @Test fun entryAt_returnsEntry_orNullForOutOfBounds() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    assertEquals("A", tracker.entryAt(0))
    assertEquals("B", tracker.entryAt(1))
    assertNull(tracker.entryAt(-1))
    assertNull(tracker.entryAt(2))
  }

  @Test fun isSelected_reflectsSelectedIndex() {
    val tracker = CandidateSelectionTracker<String>()
    tracker.show(listOf("A", "B"))
    assertTrue(tracker.isSelected(0))
    assertFalse(tracker.isSelected(1))
    tracker.click(1)
    assertFalse(tracker.isSelected(0))
    assertTrue(tracker.isSelected(1))
  }
}
