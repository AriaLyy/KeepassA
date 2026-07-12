package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.util.Date
import java.util.UUID

class EntryDifferImplTest {

  @Test
  fun diff_returnsSameWhenStringFieldValuesMatch() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "same title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "same title"))

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Same(ProtectedString(false, "same title")),
      diff.strings[PwEntryV4.STR_TITLE]
    )
  }

  @Test
  fun diff_returnsOnlyLocalWhenStringFieldExistsOnlyLocally() {
    val local = entryWithStrings(PwEntryV4.STR_USERNAME to ProtectedString(false, "local user"))
    val cloud = entryWithStrings()

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.OnlyLocal(ProtectedString(false, "local user")),
      diff.strings[PwEntryV4.STR_USERNAME]
    )
  }

  @Test
  fun diff_returnsOnlyCloudWhenStringFieldExistsOnlyInCloud() {
    val local = entryWithStrings()
    val cloud = entryWithStrings(PwEntryV4.STR_URL to ProtectedString(false, "https://cloud.example"))

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.OnlyCloud(ProtectedString(false, "https://cloud.example")),
      diff.strings[PwEntryV4.STR_URL]
    )
  }

  @Test
  fun diff_returnsConflictWhenStringFieldValuesDiffer() {
    val local = entryWithStrings(PwEntryV4.STR_NOTES to ProtectedString(false, "local notes"))
    val cloud = entryWithStrings(PwEntryV4.STR_NOTES to ProtectedString(false, "cloud notes"))

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = ProtectedString(false, "local notes"),
        cloud = ProtectedString(false, "cloud notes")
      ),
      diff.strings[PwEntryV4.STR_NOTES]
    )
  }

  @Test
  fun diff_returnsSameWhenBinaryFieldValuesMatch() {
    val local = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))
    val cloud = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))

    val diff = EntryDifferImpl().diff(local, cloud)

    val binaryDiff = diff.binaries["attachment.txt"]
    assertTrue(binaryDiff is ThreeWay.Same)
    assertArrayEquals(byteArrayOf(1, 2, 3), (binaryDiff as ThreeWay.Same).value.getData().readBytes())
  }

  @Test
  fun diff_comparesBinariesByPwEntryV4MapKeyEvenWhenContentMatches() {
    val local = entryWithBinaries("" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))
    val cloud = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))

    val diff = EntryDifferImpl().diff(local, cloud)

    val localDiff = diff.binaries[""]
    val cloudDiff = diff.binaries["attachment.txt"]
    assertTrue(localDiff is ThreeWay.OnlyLocal)
    assertTrue(cloudDiff is ThreeWay.OnlyCloud)
    assertArrayEquals(byteArrayOf(1, 2, 3), (localDiff as ThreeWay.OnlyLocal).value.getData().readBytes())
    assertArrayEquals(byteArrayOf(1, 2, 3), (cloudDiff as ThreeWay.OnlyCloud).value.getData().readBytes())
  }

  @Test
  fun diff_preservesBinaryMapKeysWhenContentMatchesButProtectedFlagDiffers() {
    val local = entryWithBinaries("" to ProtectedBinary(true, byteArrayOf(1, 2, 3)))
    val cloud = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))

    val diff = EntryDifferImpl().diff(local, cloud)

    assertTrue(diff.binaries[""] is ThreeWay.OnlyLocal)
    assertTrue(diff.binaries["attachment.txt"] is ThreeWay.OnlyCloud)
  }

  @Test
  fun diff_returnsConflictWhenSameBinaryKeyHasDifferentContent() {
    val local = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))
    val cloud = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(3, 2, 1)))

    val diff = EntryDifferImpl().diff(local, cloud)

    val binaryDiff = diff.binaries["attachment.txt"]
    assertTrue(binaryDiff is ThreeWay.Conflict)
    assertArrayEquals(byteArrayOf(1, 2, 3), (binaryDiff as ThreeWay.Conflict).local.getData().readBytes())
    assertArrayEquals(byteArrayOf(3, 2, 1), binaryDiff.cloud.getData().readBytes())
  }

  @Test
  fun diff_returnsConflictWhenSameBinaryKeyHasSameContentButProtectedFlagDiffers() {
    val local = entryWithBinaries("attachment.txt" to ProtectedBinary(true, byteArrayOf(1, 2, 3)))
    val cloud = entryWithBinaries("attachment.txt" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))

    val diff = EntryDifferImpl().diff(local, cloud)

    val binaryDiff = diff.binaries["attachment.txt"]
    assertTrue(binaryDiff is ThreeWay.Conflict)
    assertTrue((binaryDiff as ThreeWay.Conflict).local.isProtected)
    assertFalse(binaryDiff.cloud.isProtected)
  }

  @Test
  fun diff_returnsConflictWhenBinaryDataCannotBeRead() {
    val local = entryWithBinaries("broken.bin" to UnreadableProtectedBinary())
    val cloud = entryWithBinaries("broken.bin" to ProtectedBinary(false, byteArrayOf(1)))

    val diff = EntryDifferImpl().diff(local, cloud)

    val binaryDiff = diff.binaries["broken.bin"]
    assertTrue(binaryDiff is ThreeWay.Conflict)
    assertSame(local.binaries["broken.bin"], (binaryDiff as ThreeWay.Conflict).local)
    assertSame(cloud.binaries["broken.bin"], binaryDiff.cloud)
  }

  @Test
  fun diff_comparesPwEntryV4PropertiesNotRenderedByEntryDetailUi() {
    val localPrevParent = UUID(0, 10)
    val cloudPrevParent = UUID(0, 11)
    val local = PwEntryV4().apply {
      foregroundColor = "#111111"
      backgroupColor = "#222222"
      overrideURL = "local-override"
      additional = "local-additional"
      qualityCheck = true
      prevParentGroup = localPrevParent
      setLocationChanged(Date(100))
      setCreationTime(Date(200))
      setLastModificationTime(Date(300))
      setLastAccessTime(Date(400))
      setUsageCount(1)
      setPrivateUrl("local-internal-url")
      autoType.enabled = false
      autoType.obfuscationOptions = 1
      autoType.defaultSequence = "local-sequence"
      autoType.put("local-window", "local-window-sequence")
      customData = PwCustomData().apply {
        put("local-key", "local-value", Date(500))
      }
    }
    val cloud = PwEntryV4().apply {
      foregroundColor = "#333333"
      backgroupColor = "#444444"
      overrideURL = "cloud-override"
      additional = "cloud-additional"
      qualityCheck = false
      prevParentGroup = cloudPrevParent
      setLocationChanged(Date(101))
      setCreationTime(Date(201))
      setLastModificationTime(Date(301))
      setLastAccessTime(Date(401))
      setUsageCount(2)
      setPrivateUrl("cloud-internal-url")
      autoType.enabled = true
      autoType.obfuscationOptions = 2
      autoType.defaultSequence = "cloud-sequence"
      autoType.put("cloud-window", "cloud-window-sequence")
      customData = PwCustomData().apply {
        put("cloud-key", "cloud-value", Date(501))
      }
    }

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      setOf(
        EntryProperty.FOREGROUND_COLOR,
        EntryProperty.BACKGROUND_COLOR,
        EntryProperty.OVERRIDE_URL,
        EntryProperty.AUTO_TYPE,
        EntryProperty.LOCATION_CHANGED,
        EntryProperty.CREATION_TIME,
        EntryProperty.LAST_MODIFICATION_TIME,
        EntryProperty.LAST_ACCESS_TIME,
        EntryProperty.USAGE_COUNT,
        EntryProperty.INTERNAL_URL,
        EntryProperty.ADDITIONAL,
        EntryProperty.CUSTOM_DATA,
        EntryProperty.PREVIOUS_PARENT_GROUP,
        EntryProperty.QUALITY_CHECK
      ),
      diff.properties.keys
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("#111111"), EntryPropertyValue.Text("#333333")),
      diff.properties[EntryProperty.FOREGROUND_COLOR]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("#222222"), EntryPropertyValue.Text("#444444")),
      diff.properties[EntryProperty.BACKGROUND_COLOR]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("local-override"), EntryPropertyValue.Text("cloud-override")),
      diff.properties[EntryProperty.OVERRIDE_URL]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("local-additional"), EntryPropertyValue.Text("cloud-additional")),
      diff.properties[EntryProperty.ADDITIONAL]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.BooleanFlag(true), EntryPropertyValue.BooleanFlag(false)),
      diff.properties[EntryProperty.QUALITY_CHECK]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.UuidValue(localPrevParent), EntryPropertyValue.UuidValue(cloudPrevParent)),
      diff.properties[EntryProperty.PREVIOUS_PARENT_GROUP]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(100)), EntryPropertyValue.DateTime(Date(101))),
      diff.properties[EntryProperty.LOCATION_CHANGED]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(200)), EntryPropertyValue.DateTime(Date(201))),
      diff.properties[EntryProperty.CREATION_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(300)), EntryPropertyValue.DateTime(Date(301))),
      diff.properties[EntryProperty.LAST_MODIFICATION_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(400)), EntryPropertyValue.DateTime(Date(401))),
      diff.properties[EntryProperty.LAST_ACCESS_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.LongNumber(1L), EntryPropertyValue.LongNumber(2L)),
      diff.properties[EntryProperty.USAGE_COUNT]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("local-internal-url"), EntryPropertyValue.Text("cloud-internal-url")),
      diff.properties[EntryProperty.INTERNAL_URL]
    )
    assertEquals(
      ThreeWay.Conflict(
        EntryPropertyValue.AutoType(
          enabled = false,
          obfuscationOptions = 1L,
          defaultSequence = "local-sequence",
          windowSequencePairs = mapOf("local-window" to "local-window-sequence")
        ),
        EntryPropertyValue.AutoType(
          enabled = true,
          obfuscationOptions = 2L,
          defaultSequence = "cloud-sequence",
          windowSequencePairs = mapOf("cloud-window" to "cloud-window-sequence")
        )
      ),
      diff.properties[EntryProperty.AUTO_TYPE]
    )
    assertEquals(
      ThreeWay.Conflict(
        EntryPropertyValue.CustomData(
          values = mapOf("local-key" to "local-value"),
          lastModified = mapOf("local-key" to Date(500))
        ),
        EntryPropertyValue.CustomData(
          values = mapOf("cloud-key" to "cloud-value"),
          lastModified = mapOf("cloud-key" to Date(501))
        )
      ),
      diff.properties[EntryProperty.CUSTOM_DATA]
    )
  }

  @Test
  fun diff_doesNotExposePwEntryV4UuidAsMergeableContent() {
    val local = PwEntryV4().apply {
      uuid = UUID(0, 1)
    }
    val cloud = PwEntryV4().apply {
      uuid = UUID(0, 2)
    }

    val diff = EntryDifferImpl().diff(local, cloud)

    assertFalse(diff.properties.keys.any { it.name == "UUID" })
  }

  @Test
  fun diff_returnsConflictWhenStandardIconsDiffer() {
    val local = entryWithIcon(PwIconStandard(1))
    val cloud = entryWithIcon(PwIconStandard(2))

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = PwIconStandard(1),
        cloud = PwIconStandard(2)
      ),
      diff.icon
    )
  }

  @Test
  fun diff_returnsConflictWhenCustomIconsDiffer() {
    val localIcon = PwIconCustom(UUID(0, 1), byteArrayOf(1))
    val cloudIcon = PwIconCustom(UUID(0, 2), byteArrayOf(2))
    val local = entryWithCustomIcon(localIcon)
    val cloud = entryWithCustomIcon(cloudIcon)

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = localIcon,
        cloud = cloudIcon
      ),
      diff.customIcon
    )
  }

  @Test
  fun diff_returnsConflictWhenExpiryTimesDiffer() {
    val localDate = Date(1_000)
    val cloudDate = Date(2_000)
    val local = entryWithExpiryTime(localDate)
    val cloud = entryWithExpiryTime(cloudDate)

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = localDate,
        cloud = cloudDate
      ),
      diff.expireDate
    )
  }

  @Test
  fun diff_returnsConflictWhenExpiresFlagsDiffer() {
    val local = entryWithExpires(false)
    val cloud = entryWithExpires(true)

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = false,
        cloud = true
      ),
      diff.expires
    )
  }

  @Test
  fun diff_returnsConflictWhenGroupNotesDiffer() {
    val local = groupWithNotes("local notes")
    val cloud = groupWithNotes("cloud notes")

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(
        local = "local notes",
        cloud = "cloud notes"
      ),
      diff.notes
    )
  }

  @Test
  fun diff_comparesPwGroupV4PropertiesNotRenderedByGroupUi() {
    val localPrevParent = UUID(0, 20)
    val cloudPrevParent = UUID(0, 21)
    val localTopEntry = UUID(0, 30)
    val cloudTopEntry = UUID(0, 31)
    val local = PwGroupV4().apply {
      icon = PwIconStandard(0)
      isExpanded = true
      defaultAutoTypeSequence = "local-sequence"
      enableAutoType = true
      enableSearching = false
      lastTopVisibleEntry = localTopEntry
      prevParentGroup = localPrevParent
      tags = "alpha"
      setLocationChanged(Date(100))
      setCreationTime(Date(200))
      setLastModificationTime(Date(300))
      setLastAccessTime(Date(400))
      setUsageCount(1)
      customData = PwCustomData().apply {
        put("local-key", "local-value", Date(500))
      }
    }
    val cloud = PwGroupV4().apply {
      icon = PwIconStandard(0)
      isExpanded = false
      defaultAutoTypeSequence = "cloud-sequence"
      enableAutoType = false
      enableSearching = true
      lastTopVisibleEntry = cloudTopEntry
      prevParentGroup = cloudPrevParent
      tags = "beta"
      setLocationChanged(Date(101))
      setCreationTime(Date(201))
      setLastModificationTime(Date(301))
      setLastAccessTime(Date(401))
      setUsageCount(2)
      customData = PwCustomData().apply {
        put("cloud-key", "cloud-value", Date(501))
      }
    }

    val diff = EntryDifferImpl().diff(local, cloud)

    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.BooleanFlag(true), EntryPropertyValue.BooleanFlag(false)),
      diff.properties[GroupProperty.IS_EXPANDED]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.Text("local-sequence"), EntryPropertyValue.Text("cloud-sequence")),
      diff.properties[GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.NullableBooleanFlag(true), EntryPropertyValue.NullableBooleanFlag(false)),
      diff.properties[GroupProperty.ENABLE_AUTO_TYPE]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.NullableBooleanFlag(false), EntryPropertyValue.NullableBooleanFlag(true)),
      diff.properties[GroupProperty.ENABLE_SEARCHING]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.UuidValue(localTopEntry), EntryPropertyValue.UuidValue(cloudTopEntry)),
      diff.properties[GroupProperty.LAST_TOP_VISIBLE_ENTRY]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.UuidValue(localPrevParent), EntryPropertyValue.UuidValue(cloudPrevParent)),
      diff.properties[GroupProperty.PREVIOUS_PARENT_GROUP]
    )
    assertEquals(ThreeWay.Conflict("alpha", "beta"), diff.tags)
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(100)), EntryPropertyValue.DateTime(Date(101))),
      diff.properties[GroupProperty.LOCATION_CHANGED]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(200)), EntryPropertyValue.DateTime(Date(201))),
      diff.properties[GroupProperty.CREATION_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(300)), EntryPropertyValue.DateTime(Date(301))),
      diff.properties[GroupProperty.LAST_MODIFICATION_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.DateTime(Date(400)), EntryPropertyValue.DateTime(Date(401))),
      diff.properties[GroupProperty.LAST_ACCESS_TIME]
    )
    assertEquals(
      ThreeWay.Conflict(EntryPropertyValue.LongNumber(1L), EntryPropertyValue.LongNumber(2L)),
      diff.properties[GroupProperty.USAGE_COUNT]
    )
    assertEquals(
      ThreeWay.Conflict(
        EntryPropertyValue.CustomData(
          values = mapOf("local-key" to "local-value"),
          lastModified = mapOf("local-key" to Date(500))
        ),
        EntryPropertyValue.CustomData(
          values = mapOf("cloud-key" to "cloud-value"),
          lastModified = mapOf("cloud-key" to Date(501))
        )
      ),
      diff.properties[GroupProperty.CUSTOM_DATA]
    )
  }

  private fun entryWithStrings(
    vararg strings: Pair<String, ProtectedString>
  ): PwEntryV4 {
    return PwEntryV4().apply {
      this.strings = hashMapOf(*strings)
    }
  }

  private fun entryWithBinaries(
    vararg binaries: Pair<String, ProtectedBinary>
  ): PwEntryV4 {
    return PwEntryV4().apply {
      this.binaries = hashMapOf(*binaries)
    }
  }

  private fun entryWithIcon(icon: PwIconStandard): PwEntryV4 {
    return PwEntryV4().apply {
      this.icon = icon
    }
  }

  private fun entryWithCustomIcon(icon: PwIconCustom): PwEntryV4 {
    return PwEntryV4().apply {
      this.customIcon = icon
    }
  }

  private fun entryWithExpiryTime(date: Date): PwEntryV4 {
    return PwEntryV4().apply {
      this.expiryTime = date
    }
  }

  private fun entryWithExpires(expires: Boolean): PwEntryV4 {
    return PwEntryV4().apply {
      setPrivateExpires(expires)
    }
  }

  private fun groupWithNotes(notes: String): PwGroupV4 {
    return PwGroupV4().apply {
      this.notes = notes
    }
  }

  private fun PwEntryV4.setPrivateExpires(expires: Boolean) {
    val field = generateSequence(javaClass as Class<*>) { it.superclass }
      .mapNotNull { type ->
        runCatching { type.getDeclaredField("expires") }.getOrNull()
      }
      .first()
    field.isAccessible = true
    field.setBoolean(this, expires)
  }

  private fun PwEntryV4.setPrivateUrl(value: String) {
    val field = generateSequence(javaClass as Class<*>) { it.superclass }
      .mapNotNull { type ->
        runCatching { type.getDeclaredField("url") }.getOrNull()
      }
      .first()
    field.isAccessible = true
    field.set(this, value)
  }

  private class UnreadableProtectedBinary : ProtectedBinary(false, byteArrayOf(1)) {
    override fun getData(): InputStream {
      return object : InputStream() {
        override fun read(): Int {
          throw IOException("Failed to verify padding during decryption")
        }
      }
    }
  }
}
