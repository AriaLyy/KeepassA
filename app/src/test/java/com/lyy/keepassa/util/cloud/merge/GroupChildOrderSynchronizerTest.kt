package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.LinkedHashMap
import java.util.UUID

class GroupChildOrderSynchronizerTest {

  @Test
  fun apply_reordersLocalEntryInstancesByCloudOrderAndAppendsLocalOnlyEntries() {
    val localRoot = group("root", 0)
    val cloudRoot = group("root", 0)
    val localFirst = entry(1, localRoot, "first")
    val localSecond = entry(2, localRoot, "second")
    val localOnly = entry(3, localRoot, "local-only")
    val cloudFirst = entry(1, cloudRoot, "first")
    val cloudSecond = entry(2, cloudRoot, "second")
    localRoot.childEntries.addAll(listOf(localFirst, localSecond, localOnly))
    cloudRoot.childEntries.addAll(listOf(cloudSecond, cloudFirst))
    val localDb = database(localRoot, entries = listOf(localFirst, localSecond, localOnly))
    val cloudDb = database(cloudRoot, entries = listOf(cloudFirst, cloudSecond))

    val synchronizer = GroupChildOrderSynchronizer()

    assertTrue(synchronizer.hasDifferences(cloudDb, localDb))

    synchronizer.apply(cloudDb, localDb)

    assertEquals(listOf(localSecond, localFirst, localOnly), localRoot.childEntries)
    assertSame(localSecond, localRoot.childEntries[0])
    assertSame(localFirst, localRoot.childEntries[1])
    assertFalse(synchronizer.hasDifferences(cloudDb, localDb))
  }

  @Test
  fun apply_reordersLocalGroupInstancesByCloudOrderAndAppendsLocalOnlyGroups() {
    val localRoot = group("root", 0)
    val cloudRoot = group("root", 0)
    val localFirst = group("first", 1).apply { parent = localRoot }
    val localSecond = group("second", 2).apply { parent = localRoot }
    val localOnly = group("local-only", 3).apply { parent = localRoot }
    val cloudFirst = group("first", 1).apply { parent = cloudRoot }
    val cloudSecond = group("second", 2).apply { parent = cloudRoot }
    localRoot.childGroups.addAll(listOf(localFirst, localSecond, localOnly))
    cloudRoot.childGroups.addAll(listOf(cloudSecond, cloudFirst))
    val localDb = database(localRoot, groups = listOf(localFirst, localSecond, localOnly))
    val cloudDb = database(cloudRoot, groups = listOf(cloudFirst, cloudSecond))

    val synchronizer = GroupChildOrderSynchronizer()

    assertTrue(synchronizer.hasDifferences(cloudDb, localDb))

    synchronizer.apply(cloudDb, localDb)

    assertEquals(listOf(localSecond, localFirst, localOnly), localRoot.childGroups)
    assertSame(localSecond, localRoot.childGroups[0])
    assertSame(localFirst, localRoot.childGroups[1])
    assertFalse(synchronizer.hasDifferences(cloudDb, localDb))
  }

  @Test
  fun apply_doesNotMoveLocalEntryWhoseParentDoesNotMatchTheCloudGroup() {
    val localRoot = group("root", 0)
    val localOther = group("other", 9).apply { parent = localRoot }
    val cloudRoot = group("root", 0)
    val cloudOther = group("other", 9).apply { parent = cloudRoot }
    val localEntry = entry(1, localOther, "local")
    val cloudEntry = entry(1, cloudRoot, "cloud")
    localRoot.childGroups.add(localOther)
    cloudRoot.childGroups.add(cloudOther)
    localOther.childEntries.add(localEntry)
    cloudRoot.childEntries.add(cloudEntry)
    val localDb = database(localRoot, groups = listOf(localOther), entries = listOf(localEntry))
    val cloudDb = database(cloudRoot, groups = listOf(cloudOther), entries = listOf(cloudEntry))

    GroupChildOrderSynchronizer().apply(cloudDb, localDb)

    assertTrue(localRoot.childEntries.isEmpty())
    assertEquals(listOf(localEntry), localOther.childEntries)
    assertSame(localOther, localEntry.parent)
  }

  private fun database(
    root: PwGroupV4,
    groups: List<PwGroupV4> = emptyList(),
    entries: List<PwEntryV4> = emptyList()
  ): PwDatabaseV4 {
    return PwDatabaseV4().apply {
      rootGroup = root
      this.groups = linkedGroups(root, groups)
      this.entries = LinkedHashMap<UUID, PwEntry>().apply {
        entries.forEach { put(it.uuid, it) }
      }
    }
  }

  private fun linkedGroups(
    root: PwGroupV4,
    groups: List<PwGroupV4>
  ): LinkedHashMap<PwGroupId, PwGroup> {
    return LinkedHashMap<PwGroupId, PwGroup>().apply {
      put(root.id, root)
      groups.forEach { put(it.id, it) }
    }
  }

  private fun group(name: String, id: Long): PwGroupV4 {
    return PwGroupV4().apply {
      this.name = name
      icon = PwIconStandard(0)
      setId(PwGroupIdV4(UUID(0, id)))
    }
  }

  private fun entry(id: Long, parent: PwGroupV4, title: String): PwEntryV4 {
    return PwEntryV4().apply {
      uuid = UUID(0, id)
      this.parent = parent
      setString(PwEntryV4.STR_TITLE, title, false)
    }
  }
}
