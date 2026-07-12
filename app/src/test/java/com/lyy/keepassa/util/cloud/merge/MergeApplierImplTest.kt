package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.UUID

class MergeApplierImplTest {

  @Test
  fun apply_returnsNewEntryWithAutoResolvedStringWithoutMutatingLocal() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "cloud title"))
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.StringField(PwEntryV4.STR_TITLE) to FieldValue.StringValue(
          ProtectedString(false, "cloud title")
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertNotSame(local, result)
    assertEquals("cloud title", result.strings[PwEntryV4.STR_TITLE].toString())
    assertEquals("local title", local.strings[PwEntryV4.STR_TITLE].toString())
  }

  @Test
  fun apply_throwsWhenConflictFieldHasNoDecision() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "cloud title"))
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.StringField(PwEntryV4.STR_TITLE),
          local = FieldValue.StringValue(ProtectedString(false, "local title")),
          cloud = FieldValue.StringValue(ProtectedString(false, "cloud title"))
        )
      )
    )

    assertThrows(IllegalArgumentException::class.java) {
      MergeApplierImpl().apply(
        local = local,
        autoMerge = autoMerge,
        decisions = emptyMap()
      )
    }
  }

  @Test
  fun apply_usesLocalStringWhenConflictDecisionIsLocal() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "cloud title"))
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.StringField(PwEntryV4.STR_TITLE),
          local = FieldValue.StringValue(ProtectedString(false, "local title")),
          cloud = FieldValue.StringValue(ProtectedString(false, "cloud title"))
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.StringField(PwEntryV4.STR_TITLE) to Decision.LOCAL)
    ) as PwEntryV4

    assertEquals("local title", result.strings[PwEntryV4.STR_TITLE].toString())
  }

  @Test
  fun apply_usesCloudStringWhenConflictDecisionIsCloud() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "cloud title"))
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.StringField(PwEntryV4.STR_TITLE),
          local = FieldValue.StringValue(ProtectedString(false, "local title")),
          cloud = FieldValue.StringValue(ProtectedString(false, "cloud title"))
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.StringField(PwEntryV4.STR_TITLE) to Decision.CLOUD)
    ) as PwEntryV4

    assertEquals("cloud title", result.strings[PwEntryV4.STR_TITLE].toString())
    assertEquals("local title", local.strings[PwEntryV4.STR_TITLE].toString())
  }

  @Test
  fun apply_writesAutoResolvedTagsWithoutMutatingLocal() {
    val local = entryWithStrings().apply {
      tags = "alpha"
    }
    val cloud = entryWithStrings().apply {
      tags = "beta"
    }
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(FieldKey.Tags to FieldValue.TagsValue("alpha;beta")),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertEquals("alpha;beta", result.tags)
    assertEquals("alpha", local.tags)
  }

  @Test
  fun apply_writesAutoResolvedBinaryWithoutMutatingLocal() {
    val local = entryWithBinaries("file.bin" to ProtectedBinary(false, byteArrayOf(1)))
    val cloud = entryWithBinaries("file.bin" to ProtectedBinary(false, byteArrayOf(2)))
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.BinaryField("file.bin") to FieldValue.BinaryValue(
          ProtectedBinary(false, byteArrayOf(2))
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertArrayEquals(byteArrayOf(2), result.binaries["file.bin"]!!.getData().readBytes())
    assertArrayEquals(byteArrayOf(1), local.binaries["file.bin"]!!.getData().readBytes())
  }

  @Test
  fun apply_preservesDistinctBinaryMapKeysWhenAutoResolvedNamedBinaryHasSameBytes() {
    val local = entryWithBinaries("" to ProtectedBinary(false, byteArrayOf(1, 2, 3)))
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.BinaryField("attachment.txt") to FieldValue.BinaryValue(
          ProtectedBinary(false, byteArrayOf(1, 2, 3))
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertEquals(setOf("", "attachment.txt"), result.binaries.keys)
    assertArrayEquals(byteArrayOf(1, 2, 3), result.binaries[""]!!.getData().readBytes())
    assertArrayEquals(byteArrayOf(1, 2, 3), result.binaries["attachment.txt"]!!.getData().readBytes())
    assertEquals(setOf(""), local.binaries.keys)
  }

  @Test
  fun apply_preservesDistinctBinaryMapKeysWhenContentMatchesButProtectedFlagDiffers() {
    val local = entryWithBinaries("" to ProtectedBinary(true, byteArrayOf(1, 2, 3)))
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.BinaryField("attachment.txt") to FieldValue.BinaryValue(
          ProtectedBinary(false, byteArrayOf(1, 2, 3))
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertEquals(setOf("", "attachment.txt"), result.binaries.keys)
    assertArrayEquals(byteArrayOf(1, 2, 3), result.binaries[""]!!.getData().readBytes())
    assertArrayEquals(byteArrayOf(1, 2, 3), result.binaries["attachment.txt"]!!.getData().readBytes())
    assertEquals(setOf(""), local.binaries.keys)
  }

  @Test
  fun apply_writesAutoResolvedPwEntryV4PropertiesWithoutMutatingLocal() {
    val local = PwEntryV4().apply {
      foregroundColor = "#111111"
      backgroupColor = "#222222"
      overrideURL = "local-override"
      additional = "local-additional"
      qualityCheck = true
      prevParentGroup = UUID(0, 10)
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
    val cloudPrevParent = UUID(0, 11)
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.EntryPropertyField(EntryProperty.FOREGROUND_COLOR) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("#333333")),
        FieldKey.EntryPropertyField(EntryProperty.BACKGROUND_COLOR) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("#444444")),
        FieldKey.EntryPropertyField(EntryProperty.OVERRIDE_URL) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("cloud-override")),
        FieldKey.EntryPropertyField(EntryProperty.ADDITIONAL) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("cloud-additional")),
        FieldKey.EntryPropertyField(EntryProperty.QUALITY_CHECK) to
            FieldValue.PropertyValue(EntryPropertyValue.BooleanFlag(false)),
        FieldKey.EntryPropertyField(EntryProperty.PREVIOUS_PARENT_GROUP) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudPrevParent)),
        FieldKey.EntryPropertyField(EntryProperty.LOCATION_CHANGED) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(101))),
        FieldKey.EntryPropertyField(EntryProperty.CREATION_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(201))),
        FieldKey.EntryPropertyField(EntryProperty.LAST_MODIFICATION_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(301))),
        FieldKey.EntryPropertyField(EntryProperty.LAST_ACCESS_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(401))),
        FieldKey.EntryPropertyField(EntryProperty.USAGE_COUNT) to
            FieldValue.PropertyValue(EntryPropertyValue.LongNumber(2)),
        FieldKey.EntryPropertyField(EntryProperty.INTERNAL_URL) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("cloud-internal-url")),
        FieldKey.EntryPropertyField(EntryProperty.AUTO_TYPE) to FieldValue.PropertyValue(
          EntryPropertyValue.AutoType(
            enabled = true,
            obfuscationOptions = 2L,
            defaultSequence = "cloud-sequence",
            windowSequencePairs = mapOf("cloud-window" to "cloud-window-sequence")
          )
        ),
        FieldKey.EntryPropertyField(EntryProperty.CUSTOM_DATA) to FieldValue.PropertyValue(
          EntryPropertyValue.CustomData(
            values = mapOf("cloud-key" to "cloud-value"),
            lastModified = mapOf("cloud-key" to Date(501))
          )
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertEquals("#333333", result.foregroundColor)
    assertEquals("#444444", result.backgroupColor)
    assertEquals("cloud-override", result.overrideURL)
    assertEquals("cloud-additional", result.additional)
    assertFalse(result.qualityCheck)
    assertEquals(cloudPrevParent, result.prevParentGroup)
    assertEquals(Date(101), result.getLocationChanged())
    assertEquals(Date(201), result.creationTime)
    assertEquals(Date(301), result.lastModificationTime)
    assertEquals(Date(401), result.lastAccessTime)
    assertEquals(2L, result.usageCount)
    assertEquals("cloud-internal-url", result.getPrivateUrl())
    assertTrue(result.autoType.enabled)
    assertEquals(2L, result.autoType.obfuscationOptions)
    assertEquals("cloud-sequence", result.autoType.defaultSequence)
    assertEquals(mapOf("cloud-window" to "cloud-window-sequence"), result.autoType.entrySet().associate { it.key to it.value })
    assertEquals(mapOf("cloud-key" to "cloud-value"), result.customData.toMap())
    assertEquals(Date(501), result.customData.getLastMod("cloud-key"))

    assertEquals("#111111", local.foregroundColor)
    assertEquals("local-additional", local.additional)
    assertEquals("local-internal-url", local.getPrivateUrl())
  }

  @Test
  fun apply_usesCloudBinaryWhenConflictDecisionIsCloud() {
    val local = entryWithBinaries("same-name.bin" to ProtectedBinary(false, byteArrayOf(1)))
    val cloud = entryWithBinaries("same-name.bin" to ProtectedBinary(false, byteArrayOf(2)))
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.BinaryField("same-name.bin"),
          local = FieldValue.BinaryValue(ProtectedBinary(false, byteArrayOf(1))),
          cloud = FieldValue.BinaryValue(ProtectedBinary(false, byteArrayOf(2)))
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.BinaryField("same-name.bin") to Decision.CLOUD)
    ) as PwEntryV4

    assertArrayEquals(byteArrayOf(2), result.binaries["same-name.bin"]!!.getData().readBytes())
    assertEquals(setOf("same-name.bin"), result.binaries.keys)
    assertArrayEquals(byteArrayOf(1), local.binaries["same-name.bin"]!!.getData().readBytes())
  }

  @Test
  fun apply_usesCloudIconWhenConflictDecisionIsCloud() {
    val local = entryWithIcon(PwIconStandard(1))
    val cloud = entryWithIcon(PwIconStandard(2))
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.Icon,
          local = FieldValue.IconValue(PwIconStandard(1)),
          cloud = FieldValue.IconValue(PwIconStandard(2))
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.Icon to Decision.CLOUD)
    ) as PwEntryV4

    assertEquals(PwIconStandard(2), result.icon)
    assertEquals(PwIconStandard(1), local.icon)
  }

  @Test
  fun apply_usesCloudCustomIconWhenConflictDecisionIsCloud() {
    val localIcon = PwIconCustom(UUID(0, 1), byteArrayOf(1))
    val cloudIcon = PwIconCustom(UUID(0, 2), byteArrayOf(2))
    val local = entryWithCustomIcon(localIcon)
    val cloud = entryWithCustomIcon(cloudIcon)
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.CustomIcon,
          local = FieldValue.CustomIconValue(localIcon),
          cloud = FieldValue.CustomIconValue(cloudIcon)
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.CustomIcon to Decision.CLOUD)
    ) as PwEntryV4

    assertEquals(cloudIcon, result.customIcon)
    assertEquals(localIcon, local.customIcon)
  }

  @Test
  fun apply_usesCloudExpireDateWhenConflictDecisionIsCloud() {
    val localDate = Date(1_000)
    val cloudDate = Date(2_000)
    val local = entryWithExpiryTime(localDate)
    val cloud = entryWithExpiryTime(cloudDate)
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.ExpireDate,
          local = FieldValue.NullableDateValue(localDate),
          cloud = FieldValue.NullableDateValue(cloudDate)
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.ExpireDate to Decision.CLOUD)
    ) as PwEntryV4

    assertEquals(cloudDate, result.expiryTime)
    assertEquals(localDate, local.expiryTime)
  }

  @Test
  fun apply_usesCloudExpiresWhenConflictDecisionIsCloud() {
    val local = entryWithExpires(false)
    val cloud = entryWithExpires(true)
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.Expires,
          local = FieldValue.BooleanValue(false),
          cloud = FieldValue.BooleanValue(true)
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.Expires to Decision.CLOUD)
    )

    assertTrue(result.expires())
    assertFalse(local.expires())
  }

  @Test
  fun apply_pushesLocalVersionToHistoryBeforeMerging() {
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
    val cloud = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "cloud title"))
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.StringField(PwEntryV4.STR_TITLE) to FieldValue.StringValue(
          ProtectedString(false, "cloud title")
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwEntryV4

    assertEquals("cloud title", result.strings[PwEntryV4.STR_TITLE].toString())
    assertEquals("local title", result.history.single().strings[PwEntryV4.STR_TITLE].toString())
    assertTrue(local.history.isEmpty())
  }

  @Test
  fun apply_usesDatabaseHistoryLimitWhenPushingLocalVersionToHistory() {
    val older = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "older title"))
      .apply { setLastModificationTime(Date(1_000)) }
    val local = entryWithStrings(PwEntryV4.STR_TITLE to ProtectedString(false, "local title"))
      .apply {
        setLastModificationTime(Date(2_000))
        history = arrayListOf(older)
      }
    val database = PwDatabaseV4().apply {
      historyMaxItems = 1
      historyMaxSize = -1
    }
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.StringField(PwEntryV4.STR_TITLE) to FieldValue.StringValue(
          ProtectedString(false, "merged title")
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap<FieldKey, Decision>(),
      database = database
    ) as PwEntryV4

    assertEquals("merged title", result.strings[PwEntryV4.STR_TITLE].toString())
    assertEquals(1, result.history.size)
    assertEquals("local title", result.history.single().strings[PwEntryV4.STR_TITLE].toString())
    assertEquals("older title", local.history.single().strings[PwEntryV4.STR_TITLE].toString())
  }

  @Test
  fun apply_usesCloudGroupNotesWhenConflictDecisionIsCloud() {
    val local = groupWithNotes("local notes")
    val cloud = groupWithNotes("cloud notes")
    val autoMerge = AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.GroupNotes,
          local = FieldValue.TextValue("local notes"),
          cloud = FieldValue.TextValue("cloud notes")
        )
      )
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = mapOf(FieldKey.GroupNotes to Decision.CLOUD)
    ) as PwGroupV4

    assertNotSame(local, result)
    assertEquals("cloud notes", result.notes)
    assertEquals("local notes", local.notes)
  }

  @Test
  fun apply_writesAutoResolvedPwGroupV4PropertiesWithoutMutatingLocal() {
    val local = PwGroupV4().apply {
      icon = PwIconStandard(0)
      isExpanded = true
      defaultAutoTypeSequence = "local-sequence"
      enableAutoType = true
      enableSearching = false
      lastTopVisibleEntry = UUID(0, 30)
      prevParentGroup = UUID(0, 20)
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
    val cloudTopEntry = UUID(0, 31)
    val cloudPrevParent = UUID(0, 21)
    val autoMerge = AutoMergeResult(
      autoResolved = mapOf(
        FieldKey.GroupPropertyField(GroupProperty.IS_EXPANDED) to
            FieldValue.PropertyValue(EntryPropertyValue.BooleanFlag(false)),
        FieldKey.GroupPropertyField(GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE) to
            FieldValue.PropertyValue(EntryPropertyValue.Text("cloud-sequence")),
        FieldKey.GroupPropertyField(GroupProperty.ENABLE_AUTO_TYPE) to
            FieldValue.PropertyValue(EntryPropertyValue.NullableBooleanFlag(false)),
        FieldKey.GroupPropertyField(GroupProperty.ENABLE_SEARCHING) to
            FieldValue.PropertyValue(EntryPropertyValue.NullableBooleanFlag(true)),
        FieldKey.GroupPropertyField(GroupProperty.LAST_TOP_VISIBLE_ENTRY) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudTopEntry)),
        FieldKey.GroupPropertyField(GroupProperty.PREVIOUS_PARENT_GROUP) to
            FieldValue.PropertyValue(EntryPropertyValue.UuidValue(cloudPrevParent)),
        FieldKey.Tags to FieldValue.TagsValue("alpha;beta"),
        FieldKey.GroupPropertyField(GroupProperty.LOCATION_CHANGED) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(101))),
        FieldKey.GroupPropertyField(GroupProperty.CREATION_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(201))),
        FieldKey.GroupPropertyField(GroupProperty.LAST_MODIFICATION_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(301))),
        FieldKey.GroupPropertyField(GroupProperty.LAST_ACCESS_TIME) to
            FieldValue.PropertyValue(EntryPropertyValue.DateTime(Date(401))),
        FieldKey.GroupPropertyField(GroupProperty.USAGE_COUNT) to
            FieldValue.PropertyValue(EntryPropertyValue.LongNumber(2)),
        FieldKey.GroupPropertyField(GroupProperty.CUSTOM_DATA) to FieldValue.PropertyValue(
          EntryPropertyValue.CustomData(
            values = mapOf("cloud-key" to "cloud-value"),
            lastModified = mapOf("cloud-key" to Date(501))
          )
        )
      ),
      conflicts = emptyList()
    )

    val result = MergeApplierImpl().apply(
      local = local,
      autoMerge = autoMerge,
      decisions = emptyMap()
    ) as PwGroupV4

    assertFalse(result.isExpanded)
    assertEquals("cloud-sequence", result.defaultAutoTypeSequence)
    assertFalse(result.enableAutoType!!)
    assertTrue(result.enableSearching!!)
    assertEquals(cloudTopEntry, result.lastTopVisibleEntry)
    assertEquals(cloudPrevParent, result.prevParentGroup)
    assertEquals("alpha;beta", result.tags)
    assertEquals(Date(101), result.getLocationChanged())
    assertEquals(Date(201), result.creationTime)
    assertEquals(Date(301), result.lastModificationTime)
    assertEquals(Date(401), result.lastAccessTime)
    assertEquals(2L, result.usageCount)
    assertEquals(mapOf("cloud-key" to "cloud-value"), result.customData.toMap())
    assertEquals(Date(501), result.customData.getLastMod("cloud-key"))

    assertTrue(local.isExpanded)
    assertEquals("local-sequence", local.defaultAutoTypeSequence)
    assertEquals("alpha", local.tags)
    assertEquals(mapOf("local-key" to "local-value"), local.customData.toMap())
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
      this.icon = PwIconStandard(0)
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
    privateUrlField().set(this, value)
  }

  private fun PwEntryV4.getPrivateUrl(): String {
    return privateUrlField().get(this) as String
  }

  private fun PwEntryV4.privateUrlField() = generateSequence(javaClass as Class<*>) { it.superclass }
    .mapNotNull { type ->
      runCatching { type.getDeclaredField("url") }.getOrNull()
    }
    .first()
    .apply { isAccessible = true }
}
