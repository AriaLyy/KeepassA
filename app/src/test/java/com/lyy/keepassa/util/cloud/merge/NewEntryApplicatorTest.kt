package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.UUID

class NewEntryApplicatorTest {

  @Test
  fun plan_addsParentBeforeChildWhenNewListHasChildFirst() {
    val root = group("root", UUID(0, 0))
    val parent = group("parent", UUID(0, 1)).apply { this.parent = root }
    val child = group("child", UUID(0, 2)).apply { this.parent = parent }
    val cloudEntry = entry(UUID(0, 10), child, "in child")
    val localDb = database(root)

    val newList = arrayListOf<PwDataInf>(child, parent, cloudEntry)

    val result = NewEntryApplicator().plan(newList, localDb)

    assertEquals(2, result.groupAdds.size)
    assertEquals(parentId(UUID(0, 1)), result.groupAdds[0].id)
    assertEquals(parentId(UUID(0, 2)), result.groupAdds[1].id)
    assertSame(result.groupAdds[0], result.groupAdds[1].parent)
    assertEquals(1, result.entryAdds.size)
    assertSame(result.groupAdds[1], result.entryAdds[0].parent)
    assertTrue(result.orphanGroups.isEmpty())
    assertTrue(result.orphanEntries.isEmpty())
  }

  @Test
  fun plan_addsAncestorsBeforeDescendantsAtAnyDepthAndOrder() {
    val root = group("root", UUID(0, 0))
    val level1 = group("level1", UUID(0, 1)).apply { parent = root }
    val level2 = group("level2", UUID(0, 2)).apply { parent = level1 }
    val level3 = group("level3", UUID(0, 3)).apply { parent = level2 }
    val leafEntry = entry(UUID(0, 20), level3, "deep")
    val localDb = database(root)

    val newList = arrayListOf<PwDataInf>(
      level3, level1, leafEntry, level2
    )

    val result = NewEntryApplicator().plan(newList, localDb)

    assertEquals(3, result.groupAdds.size)
    assertEquals(parentId(UUID(0, 1)), result.groupAdds[0].id)
    assertEquals(parentId(UUID(0, 2)), result.groupAdds[1].id)
    assertEquals(parentId(UUID(0, 3)), result.groupAdds[2].id)
    assertSame(result.groupAdds[0], result.groupAdds[1].parent)
    assertSame(result.groupAdds[1], result.groupAdds[2].parent)
    assertEquals(1, result.entryAdds.size)
    assertSame(result.groupAdds[2], result.entryAdds[0].parent)
  }

  @Test
  fun plan_fallsBackTopOrphanGroupToRootAndKeepsEntryInsideIt() {
    val root = group("root", UUID(0, 0))
    val missingParent = group("missing", UUID(0, 9))
    val orphanGroup = group("orphan", UUID(0, 1)).apply { parent = missingParent }
    val orphanEntry = entry(UUID(0, 11), orphanGroup, "in orphan")
    val localDb = database(root)

    val newList = arrayListOf<PwDataInf>(orphanGroup, orphanEntry)

    val result = NewEntryApplicator().plan(newList, localDb)

    assertEquals(1, result.groupAdds.size)
    assertSame(root, result.groupAdds.single().parent)
    assertEquals(1, result.entryAdds.size)
    assertSame(result.groupAdds.single(), result.entryAdds.single().parent)
    assertEquals(1, result.orphanGroups.size)
    assertSame(orphanGroup, result.orphanGroups[0])
    assertTrue(result.orphanEntries.isEmpty())
  }

  @Test
  fun plan_deepCopiesEntryFieldsAndDoesNotShareStateWithCloudSource() {
    val root = group("root", UUID(0, 0))
    val cloudEntry = entry(UUID(0, 50), root, "secret title").apply {
      setString(PwEntryV4.STR_USERNAME, "alice", true)
      setString(PwEntryV4.STR_PASSWORD, "p@ssw0rd", true)
    }
    val localDb = database(root)

    val result = NewEntryApplicator().plan(listOf(cloudEntry), localDb)

    assertEquals(1, result.entryAdds.size)
    val clone = result.entryAdds[0]
    assertNotSame(cloudEntry, clone)
    assertEquals("secret title", clone.title)
    assertEquals("alice", clone.strings[PwEntryV4.STR_USERNAME]?.toString())
    assertEquals("p@ssw0rd", clone.strings[PwEntryV4.STR_PASSWORD]?.toString())
    cloudEntry.setString(PwEntryV4.STR_PASSWORD, "mutated", true)
    assertEquals("p@ssw0rd", clone.strings[PwEntryV4.STR_PASSWORD]?.toString())
  }

  @Test
  fun plan_deepCopiesMutablePwEntryV4Containers() {
    val root = group("root", UUID(0, 0))
    val historyEntry = entry(UUID(0, 49), root, "history")
    val cloudEntry = entry(UUID(0, 50), root, "entry").apply {
      binaries["attachment.bin"] = ProtectedBinary(false, byteArrayOf(1, 2, 3))
      autoType.put("window", "sequence")
      customData = PwCustomData().apply {
        put("cloud-key", "cloud-value", Date(100))
      }
      history.add(historyEntry)
    }
    val localDb = database(root)

    val clone = NewEntryApplicator().plan(listOf(cloudEntry), localDb).entryAdds.single()

    assertNotSame(cloudEntry.binaries, clone.binaries)
    assertNotSame(cloudEntry.autoType, clone.autoType)
    assertNotSame(cloudEntry.customData, clone.customData)
    assertNotSame(cloudEntry.history, clone.history)
    clone.binaries.remove("attachment.bin")
    clone.customData["local-key"] = "local-value"
    assertTrue(cloudEntry.binaries.containsKey("attachment.bin"))
    assertFalse(cloudEntry.customData.containsKey("local-key"))
    assertArrayEquals(
      byteArrayOf(1, 2, 3),
      cloudEntry.binaries["attachment.bin"]!!.getData().readBytes()
    )
  }

  @Test
  fun plan_clearsChildCollectionsOnClonedGroups() {
    val root = group("root", UUID(0, 0))
    val parent = group("parent", UUID(0, 1)).apply { this.parent = root }
    val childEntry = entry(UUID(0, 60), parent, "in parent")
    val localDb = database(root)

    val result = NewEntryApplicator().plan(listOf(parent, childEntry), localDb)

    assertEquals(1, result.groupAdds.size)
    val clone = result.groupAdds[0]
    assertTrue(clone.childGroups.isNullOrEmpty())
    assertTrue(clone.childEntries.isNullOrEmpty())
  }

  @Test
  fun plan_doesNotClearChildCollectionsOnCloudSourceGroup() {
    val root = group("root", UUID(0, 0))
    val parent = group("parent", UUID(0, 1)).apply { this.parent = root }
    val child = group("child", UUID(0, 2)).apply { this.parent = parent }
    val childEntry = entry(UUID(0, 60), parent, "in parent")
    parent.childGroups.add(child)
    parent.childEntries.add(childEntry)
    val localDb = database(root)

    val result = NewEntryApplicator().plan(listOf(parent, child, childEntry), localDb)

    val parentClone = result.groupAdds.first { it.id == parent.id }
    assertTrue(parentClone.childGroups.isEmpty())
    assertTrue(parentClone.childEntries.isEmpty())
    assertEquals(listOf(child), parent.childGroups)
    assertEquals(listOf(childEntry), parent.childEntries)
  }

  @Test
  fun plan_preservesHierarchyBelowOrphanGroupFallback() {
    val root = group("root", UUID(0, 0))
    val missingParent = group("missing", UUID(0, 9))
    val orphanParent = group("orphan-parent", UUID(0, 1)).apply { parent = missingParent }
    val orphanChild = group("orphan-child", UUID(0, 2)).apply { parent = orphanParent }
    val cloudEntry = entry(UUID(0, 61), orphanChild, "in orphan child")
    val localDb = database(root)

    val result = NewEntryApplicator().plan(
      listOf(orphanChild, cloudEntry, orphanParent),
      localDb
    )

    assertEquals(2, result.groupAdds.size)
    val parentClone = result.groupAdds.first { it.id == orphanParent.id }
    val childClone = result.groupAdds.first { it.id == orphanChild.id }
    assertSame(root, parentClone.parent)
    assertSame(parentClone, childClone.parent)
    assertSame(childClone, result.entryAdds.single().parent)
    assertEquals(listOf(orphanParent), result.orphanGroups)
    assertTrue(result.orphanEntries.isEmpty())
  }

  @Test
  fun plannedAddsBuildCorrectDatabaseMapsAndChildCollections() {
    val root = group("root", UUID(0, 0))
    val parent = group("parent", UUID(0, 1)).apply { this.parent = root }
    val child = group("child", UUID(0, 2)).apply { this.parent = parent }
    val cloudEntry = entry(UUID(0, 62), child, "in child")
    val localDb = database(root)

    val result = NewEntryApplicator().plan(listOf(child, cloudEntry, parent), localDb)
    result.groupAdds.forEach { localDb.addGroupTo(it, it.parent) }
    result.entryAdds.forEach { localDb.addEntryTo(it, it.parent) }

    val localParent = localDb.groups[parent.id] as PwGroupV4
    val localChild = localDb.groups[child.id] as PwGroupV4
    val localEntry = localDb.entries[cloudEntry.uuid] as PwEntryV4
    assertSame(root, localParent.parent)
    assertSame(localParent, localChild.parent)
    assertSame(localChild, localEntry.parent)
    assertTrue(root.childGroups.contains(localParent))
    assertTrue(localParent.childGroups.contains(localChild))
    assertTrue(localChild.childEntries.contains(localEntry))
  }

  private fun parentId(id: UUID): PwGroupId = PwGroupIdV4(id)

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
}
