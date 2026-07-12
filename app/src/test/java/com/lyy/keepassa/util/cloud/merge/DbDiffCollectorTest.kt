package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.LinkedHashMap
import java.util.UUID

class DbDiffCollectorTest {

  @Test
  fun collect_includesMovedAndModifiedEntryInMoveAndModifyLists() {
    val entryId = UUID(0, 10)
    val localParent = group("local-parent", UUID(0, 1))
    val cloudParent = group("cloud-parent", UUID(0, 2))
    val localEntry = entry(entryId, localParent, "local title")
    val cloudEntry = entry(entryId, cloudParent, "cloud title")
    val localDb = database(localParent, entries = listOf(localEntry))
    val cloudDb = database(cloudParent, entries = listOf(cloudEntry))

    val result = DbDiffCollector().collect(cloudDb, localDb)

    assertEquals(1, result.moveList.size)
    assertSame(cloudEntry, result.moveList.single().cloudPwData)
    assertSame(localEntry, result.moveList.single().localPwData)
    assertEquals(1, result.modifyList.size)
    assertSame(cloudEntry, result.modifyList.single().first)
    assertSame(localEntry, result.modifyList.single().second)
  }

  @Test
  fun collect_returnsLocalOnlyEntriesInReverseDatabaseOrder() {
    val root = group("root", UUID(0, 0))
    val first = entry(UUID(0, 1), root, "first")
    val second = entry(UUID(0, 2), root, "second")
    val localDb = database(root, entries = listOf(first, second))
    val cloudDb = database(root)

    val result = DbDiffCollector().collect(cloudDb, localDb)

    assertEquals(listOf(second, first), result.delList)
  }

  @Test
  fun collect_returnsLocalOnlyGroupsInReverseDatabaseOrderAndSkipsRoot() {
    val root = group("root", UUID(0, 0))
    val first = group("first", UUID(0, 1)).apply { parent = root }
    val second = group("second", UUID(0, 2)).apply { parent = root }
    val localDb = database(root, groups = listOf(first, second))
    val cloudDb = database(root)

    val result = DbDiffCollector().collect(cloudDb, localDb)

    assertEquals(listOf(second, first), result.delList)
  }

  private fun database(
    root: PwGroupV4 = group("root", UUID(0, 0)),
    groups: List<PwGroupV4> = emptyList(),
    entries: List<PwEntryV4> = emptyList()
  ): PwDatabaseV4 {
    return PwDatabaseV4().apply {
      rootGroup = root
      this.groups = linkedMapOf(root.id to root).apply {
        groups.forEach { put(it.id, it) }
      }
      this.entries = linkedMapOf<UUID, com.keepassdroid.database.PwEntry>().apply {
        entries.forEach { put(it.uuid, it) }
      }
    }
  }

  private fun group(
    name: String,
    id: UUID
  ): PwGroupV4 {
    return PwGroupV4().apply {
      this.name = name
      this.icon = PwIconStandard(0)
      setId(PwGroupIdV4(id))
    }
  }

  private fun entry(
    id: UUID,
    parent: PwGroupV4,
    title: String
  ): PwEntryV4 {
    return PwEntryV4().apply {
      uuid = id
      this.parent = parent
      setString(PwEntryV4.STR_TITLE, title, false)
    }
  }

  private fun linkedMapOf(vararg pairs: Pair<PwGroupId, PwGroup>): LinkedHashMap<PwGroupId, PwGroup> {
    return LinkedHashMap<PwGroupId, PwGroup>().apply {
      pairs.forEach { (key, value) -> put(key, value) }
    }
  }
}
