package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbMergeDelegateAutomaticChangeOrderTest {

  @Test
  fun compareDb_appliesNewAndMovedItemsOnlyAfterConflictResolution() {
    val source = File(
      "src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt"
    ).readText()
    val compareStart = source.indexOf("suspend fun compareDb(")
    val mergePreparation = source.indexOf("val mergeItems = buildMergeItems", compareStart)
    val beforeMergeDecision = source.substring(compareStart, mergePreparation)

    assertFalse(beforeMergeDecision.contains("localAddNewEntry(newList, localDb)"))
    assertFalse(beforeMergeDecision.contains("moveLocalEntry(moveList, localDb)"))
    assertTrue(source.contains("private suspend fun applyAutomaticChanges("))

    val compareEnd = source.indexOf("private suspend fun applyAutomaticChanges(", mergePreparation)
    val compareBody = source.substring(compareStart, compareEnd)
    val applyCalls = Regex("(?m)^\\s+applyAutomaticChanges\\(")
      .findAll(compareBody)
      .count()
    assertEquals(2, applyCalls)

    val cancelCheck = compareBody.indexOf("if (result !is MergeConflictResult.Resolved)")
    val resolvedApply = compareBody.lastIndexOf("applyAutomaticChanges(")
    assertTrue(resolvedApply > cancelCheck)
    assertTrue(source.contains("GroupChildOrderSynchronizer().hasDifferences(cloudDb, localDb)"))
    assertTrue(source.contains("GroupChildOrderSynchronizer().apply(cloudDb, localDb)"))
  }

  @Test
  fun buildMergeItems_routesNonV4ItemsToWholeItemFallback() {
    val source = File(
      "src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt"
    ).readText()

    assertTrue(source.contains("LegacyItemMerger.conflict(local, cloud)"))
    assertTrue(source.contains("decisions[FieldKey.LegacyItem]"))
    assertTrue(source.contains("LegacyItemMerger.apply(item.local, item.cloud, decision)"))
  }
}
