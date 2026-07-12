package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import java.util.LinkedHashMap
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseContentComparatorTest {

  @Test
  fun sameContent_returnsTrue() {
    val local = database("title")
    val cloud = database("title")

    assertTrue(DatabaseContentComparator().sameContent(cloud, local))
  }

  @Test
  fun changedEntryField_returnsFalse() {
    val local = database("local")
    val cloud = database("cloud")

    assertFalse(DatabaseContentComparator().sameContent(cloud, local))
  }

  @Test
  fun changedChildOrder_returnsFalse() {
    val local = database("title", reverseEntries = false)
    val cloud = database("title", reverseEntries = true)

    assertFalse(DatabaseContentComparator().sameContent(cloud, local))
  }

  private fun database(title: String, reverseEntries: Boolean = false): PwDatabaseV4 {
    val root = PwGroupV4().apply {
      name = "root"
      icon = PwIconStandard(0)
      setId(PwGroupIdV4(UUID(0, 1)))
    }
    val first = entry(UUID(0, 10), root, title)
    val second = entry(UUID(0, 11), root, "second")
    root.childEntries.addAll(if (reverseEntries) listOf(second, first) else listOf(first, second))
    return PwDatabaseV4().apply {
      rootGroup = root
      groups = LinkedHashMap<PwGroupId, PwGroup>().apply { put(root.id, root) }
      entries = LinkedHashMap<UUID, PwEntry>().apply {
        put(first.uuid, first)
        put(second.uuid, second)
      }
    }
  }

  private fun entry(id: UUID, parent: PwGroupV4, title: String) = PwEntryV4().apply {
    uuid = id
    this.parent = parent
    setString(PwEntryV4.STR_TITLE, title, false)
  }
}
