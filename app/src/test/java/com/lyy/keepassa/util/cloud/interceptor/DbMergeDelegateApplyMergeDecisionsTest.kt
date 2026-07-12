package com.lyy.keepassa.util.cloud.interceptor

import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.util.cloud.merge.AutoMergeResult
import com.lyy.keepassa.util.cloud.merge.Decision
import com.lyy.keepassa.util.cloud.merge.EntryProperty
import com.lyy.keepassa.util.cloud.merge.EntryPropertyValue
import com.lyy.keepassa.util.cloud.merge.FieldKey
import com.lyy.keepassa.util.cloud.merge.FieldValue
import com.lyy.keepassa.util.cloud.merge.GroupProperty
import com.lyy.keepassa.util.cloud.merge.MergeConflictItem
import com.keepassdroid.database.PwGroupV4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date
import java.util.UUID

class DbMergeDelegateApplyMergeDecisionsTest {

  @Test
  fun applyMergeDecisionsCopiesPwEntryV4FieldsThatKeepassAssignDoesNotCopy() {
    val local = PwEntryV4().apply {
      tags = "local-tag"
      qualityCheck = true
      prevParentGroup = UUID(0, 1)
      customData = PwCustomData().apply {
        put("local-key", "local-value", Date(100))
      }
    }
    val cloud = PwEntryV4()
    val cloudPrevParent = UUID(0, 2)
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.Tags to FieldValue.TagsValue("cloud-tag"),
        FieldKey.EntryPropertyField(EntryProperty.QUALITY_CHECK) to
            FieldValue.PropertyValue(EntryPropertyValue.BooleanFlag(false)),
        FieldKey.EntryPropertyField(EntryProperty.PREVIOUS_PARENT_GROUP) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudPrevParent)),
        FieldKey.EntryPropertyField(EntryProperty.CUSTOM_DATA) to FieldValue.PropertyValue(
          EntryPropertyValue.CustomData(
            values = mapOf("cloud-key" to "cloud-value"),
            lastModified = mapOf("cloud-key" to Date(200))
          )
        )
      ),
      conflicts = emptyList()
    )

    invokeApplyMergeDecisions(
      items = listOf(MergeConflictItem(cloud = cloud, local = local, autoMerge = autoMerge)),
      localDb = PwDatabaseV4()
    )

    assertEquals("cloud-tag", local.tags)
    assertFalse(local.qualityCheck)
    assertEquals(cloudPrevParent, local.prevParentGroup)
    assertEquals(mapOf("cloud-key" to "cloud-value"), local.customData.toMap())
    assertEquals(Date(200), local.customData.getLastMod("cloud-key"))
  }

  @Test
  fun applyMergeDecisionsCopiesPwGroupV4FieldsThatKeepassAssignDoesNotCopy() {
    val local = PwGroupV4().apply {
      icon = PwIconStandard(0)
      tags = "local-tag"
      lastTopVisibleEntry = UUID(0, 1)
      prevParentGroup = UUID(0, 2)
      customData = PwCustomData().apply {
        put("local-key", "local-value", Date(100))
      }
    }
    val cloud = PwGroupV4().apply {
      icon = PwIconStandard(0)
    }
    val cloudTopEntry = UUID(0, 3)
    val cloudPrevParent = UUID(0, 4)
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.Tags to FieldValue.TagsValue("cloud-tag"),
        FieldKey.GroupPropertyField(GroupProperty.LAST_TOP_VISIBLE_ENTRY) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudTopEntry)),
        FieldKey.GroupPropertyField(GroupProperty.PREVIOUS_PARENT_GROUP) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudPrevParent)),
        FieldKey.GroupPropertyField(GroupProperty.CUSTOM_DATA) to FieldValue.PropertyValue(
          EntryPropertyValue.CustomData(
            values = mapOf("cloud-key" to "cloud-value"),
            lastModified = mapOf("cloud-key" to Date(200))
          )
        )
      ),
      conflicts = emptyList()
    )

    invokeApplyMergeDecisions(
      items = listOf(MergeConflictItem(cloud = cloud, local = local, autoMerge = autoMerge)),
      localDb = PwDatabaseV4()
    )

    assertEquals("cloud-tag", local.tags)
    assertEquals(cloudTopEntry, local.lastTopVisibleEntry)
    assertEquals(cloudPrevParent, local.prevParentGroup)
    assertEquals(mapOf("cloud-key" to "cloud-value"), local.customData.toMap())
    assertEquals(Date(200), local.customData.getLastMod("cloud-key"))
  }

  private fun invokeApplyMergeDecisions(
    items: List<MergeConflictItem>,
    localDb: PwDatabase
  ) {
    val method = DbMergeDelegate::class.java.getDeclaredMethod(
      "applyMergeDecisions",
      List::class.java,
      Map::class.java,
      PwDatabase::class.java
    )
    method.isAccessible = true
    method.invoke(
      DbMergeDelegate,
      items,
      emptyMap<Int, Map<FieldKey, Decision>>(),
      localDb
    )
  }
}
