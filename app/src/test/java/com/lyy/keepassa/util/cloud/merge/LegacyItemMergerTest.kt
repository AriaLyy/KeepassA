package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwGroupV3
import com.keepassdroid.database.PwIconStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyItemMergerTest {

  @Test
  fun conflict_requiresOneWholeItemDecisionForV3Entry() {
    val localParent = group(1, "local-parent")
    val cloudParent = group(1, "cloud-parent")
    val local = entry(localParent, "local")
    val cloud = entry(cloudParent, "cloud").apply { setUUID(local.uuid) }

    val result = LegacyItemMerger.conflict(local, cloud)

    assertEquals(FieldKey.LegacyItem, result.conflicts.single().key)
    assertTrue(result.conflicts.single().local is FieldValue.LegacyItemValue)
    assertTrue(result.conflicts.single().cloud is FieldValue.LegacyItemValue)
  }

  @Test
  fun differs_comparesV3PasswordAndBinaryContentInsteadOfArrayIdentity() {
    val parent = group(1, "parent")
    val local = entry(parent, "same").apply {
      setPassword(byteArrayOf(1, 2, 3), 0, 3)
      setBinaryData(byteArrayOf(4, 5, 6), 0, 3)
      binaryDesc = "file.bin"
    }
    val cloud = entry(parent, "same").apply {
      setUUID(local.uuid)
      setCreationTime(local.creationTime)
      setLastModificationTime(local.lastModificationTime)
      setLastAccessTime(local.lastAccessTime)
      setExpiryTime(local.expiryTime)
      setPassword(byteArrayOf(1, 2, 3), 0, 3)
      setBinaryData(byteArrayOf(4, 5, 6), 0, 3)
      binaryDesc = "file.bin"
    }

    assertTrue(!LegacyItemMerger.differs(local, cloud))

    cloud.additional = "changed notes"

    assertTrue(LegacyItemMerger.differs(local, cloud))
  }

  @Test
  fun applyCloudEntry_usesCloudContentButPreservesLocalIdentityAndParent() {
    val localParent = group(1, "local-parent")
    val cloudParent = group(1, "cloud-parent")
    val local = entry(localParent, "local")
    val cloud = entry(cloudParent, "cloud")
    val localId = local.uuid

    val merged = LegacyItemMerger.apply(local, cloud, Decision.CLOUD) as PwEntryV3

    assertEquals("cloud", merged.title)
    assertEquals(localId, merged.uuid)
    assertSame(localParent, merged.parent)
    assertNotSame(cloud, merged)
  }

  @Test
  fun applyCloudGroup_preservesLocalIdentityParentAndChildCollections() {
    val localParent = group(1, "local-parent")
    val cloudParent = group(1, "cloud-parent")
    val local = group(2, "local").apply { parent = localParent }
    val cloud = group(2, "cloud").apply { parent = cloudParent }
    val localChildGroups = local.childGroups
    val localChildEntries = local.childEntries

    val merged = LegacyItemMerger.apply(local, cloud, Decision.CLOUD) as PwGroupV3

    assertEquals("cloud", merged.name)
    assertEquals(local.id, merged.id)
    assertSame(localParent, merged.parent)
    assertSame(localChildGroups, merged.childGroups)
    assertSame(localChildEntries, merged.childEntries)
    assertNotSame(cloud, merged)
  }

  private fun group(id: Int, name: String): PwGroupV3 {
    return PwGroupV3().apply {
      groupId = id
      this.name = name
      icon = PwIconStandard(0)
      level = if (id == 1) 0 else 1
    }
  }

  private fun entry(parent: PwGroupV3, title: String): PwEntryV3 {
    return PwEntryV3(parent, true, true).apply {
      this.title = title
      icon = PwIconStandard(0)
    }
  }
}
