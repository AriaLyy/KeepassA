package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.UUID

class AutoMergerImplTest {

  @Test
  fun merge_autoResolvesSameStringField() {
    val title = ProtectedString(false, "same title")
    val diff = emptyEntryDiff().copy(
      strings = mapOf("Title" to ThreeWay.Same(title))
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.StringValue(title),
      result.autoResolved[FieldKey.StringField("Title")]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_collectsConflictingStringFieldForUserDecision() {
    val local = ProtectedString(false, "local title")
    val cloud = ProtectedString(false, "cloud title")
    val diff = emptyEntryDiff().copy(
      strings = mapOf("Title" to ThreeWay.Conflict(local, cloud))
    )

    val result = AutoMergerImpl().merge(diff)

    assertFalse(result.autoResolved.containsKey(FieldKey.StringField("Title")))
    assertEquals(
      listOf(
        FieldConflict(
          key = FieldKey.StringField("Title"),
          local = FieldValue.StringValue(local),
          cloud = FieldValue.StringValue(cloud),
          renderer = ConflictRenderer.PROTECTED_STRING
        )
      ),
      result.conflicts
    )
  }

  @Test
  fun merge_autoResolvesOnlyLocalStringField() {
    val local = ProtectedString(false, "local custom")
    val diff = emptyEntryDiff().copy(
      strings = mapOf("Custom" to ThreeWay.OnlyLocal(local))
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.StringValue(local),
      result.autoResolved[FieldKey.StringField("Custom")]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesOnlyCloudStringField() {
    val cloud = ProtectedString(false, "cloud custom")
    val diff = emptyEntryDiff().copy(
      strings = mapOf("Custom" to ThreeWay.OnlyCloud(cloud))
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.StringValue(cloud),
      result.autoResolved[FieldKey.StringField("Custom")]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesSameBinaryField() {
    val binary = ProtectedBinary(false, byteArrayOf(1, 2, 3))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("attachment.txt" to ThreeWay.Same(binary))
    )

    val result = AutoMergerImpl().merge(diff)

    val value = result.autoResolved[FieldKey.BinaryField("attachment.txt")]
    assertTrue(value is FieldValue.BinaryValue)
    assertArrayEquals(
      byteArrayOf(1, 2, 3),
      (value as FieldValue.BinaryValue).value.getData().readBytes()
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesOnlyLocalBinaryField() {
    val binary = ProtectedBinary(false, byteArrayOf(4, 5, 6))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("local.bin" to ThreeWay.OnlyLocal(binary))
    )

    val result = AutoMergerImpl().merge(diff)

    val value = result.autoResolved[FieldKey.BinaryField("local.bin")]
    assertTrue(value is FieldValue.BinaryValue)
    assertArrayEquals(
      byteArrayOf(4, 5, 6),
      (value as FieldValue.BinaryValue).value.getData().readBytes()
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesOnlyCloudBinaryField() {
    val binary = ProtectedBinary(false, byteArrayOf(7, 8, 9))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("cloud.bin" to ThreeWay.OnlyCloud(binary))
    )

    val result = AutoMergerImpl().merge(diff)

    val value = result.autoResolved[FieldKey.BinaryField("cloud.bin")]
    assertTrue(value is FieldValue.BinaryValue)
    assertArrayEquals(
      byteArrayOf(7, 8, 9),
      (value as FieldValue.BinaryValue).value.getData().readBytes()
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_collectsConflictingPwEntryV4PropertyForUserDecision() {
    val diff = emptyEntryDiff().copy(
      properties = mapOf(
        EntryProperty.FOREGROUND_COLOR to ThreeWay.Conflict(
          local = EntryPropertyValue.Text("#111111"),
          cloud = EntryPropertyValue.Text("#222222")
        )
      )
    )

    val result = AutoMergerImpl().merge(diff)

    assertFalse(result.autoResolved.containsKey(FieldKey.EntryPropertyField(EntryProperty.FOREGROUND_COLOR)))
    assertEquals(
      FieldConflict(
        key = FieldKey.EntryPropertyField(EntryProperty.FOREGROUND_COLOR),
        local = FieldValue.PropertyValue(EntryPropertyValue.Text("#111111")),
        cloud = FieldValue.PropertyValue(EntryPropertyValue.Text("#222222")),
        renderer = ConflictRenderer.ENTRY_PROPERTY
      ),
      result.conflicts.single()
    )
  }

  @Test
  fun merge_autoResolvesSamePwEntryV4Property() {
    val diff = emptyEntryDiff().copy(
      properties = mapOf(
        EntryProperty.USAGE_COUNT to ThreeWay.Same(EntryPropertyValue.LongNumber(3))
      )
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.PropertyValue(EntryPropertyValue.LongNumber(3)),
      result.autoResolved[FieldKey.EntryPropertyField(EntryProperty.USAGE_COUNT)]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesWhitelistedEntryPropertiesWithCloudValues() {
    val properties = linkedMapOf(
      EntryProperty.LAST_MODIFICATION_TIME to ThreeWay.Conflict(
        EntryPropertyValue.DateTime(java.util.Date(1)),
        EntryPropertyValue.DateTime(java.util.Date(2))
      ),
      EntryProperty.LAST_ACCESS_TIME to ThreeWay.Conflict(
        EntryPropertyValue.DateTime(java.util.Date(3)),
        EntryPropertyValue.DateTime(java.util.Date(4))
      ),
      EntryProperty.USAGE_COUNT to ThreeWay.Conflict(
        EntryPropertyValue.LongNumber(5),
        EntryPropertyValue.LongNumber(6)
      )
    )

    val result = AutoMergerImpl().merge(emptyEntryDiff().copy(properties = properties))

    properties.forEach { (property, difference) ->
      val cloudValue = (difference as ThreeWay.Conflict).cloud
      assertEquals(
        FieldValue.PropertyValue(cloudValue),
        result.autoResolved[FieldKey.EntryPropertyField(property)]
      )
    }
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_collectsConflictingBinaryFieldForUserDecision() {
    val local = ProtectedBinary(false, byteArrayOf(1))
    val cloud = ProtectedBinary(false, byteArrayOf(2))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("same-name.bin" to ThreeWay.Conflict(local, cloud))
    )

    val result = AutoMergerImpl().merge(diff)

    assertFalse(result.autoResolved.containsKey(FieldKey.BinaryField("same-name.bin")))
    assertEquals(FieldKey.BinaryField("same-name.bin"), result.conflicts.single().key)
    assertArrayEquals(
      byteArrayOf(1),
      (result.conflicts.single().local as FieldValue.BinaryValue).value.getData().readBytes()
    )
    assertArrayEquals(
      byteArrayOf(2),
      (result.conflicts.single().cloud as FieldValue.BinaryValue).value.getData().readBytes()
    )
  }

  @Test
  fun merge_preservesBinaryFileNameForConflictValueRendering() {
    val local = ProtectedBinary(false, byteArrayOf(1))
    val cloud = ProtectedBinary(false, byteArrayOf(2))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("same-name.bin" to ThreeWay.Conflict(local, cloud))
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals("same-name.bin", (result.conflicts.single().local as FieldValue.BinaryValue).name)
    assertEquals("same-name.bin", (result.conflicts.single().cloud as FieldValue.BinaryValue).name)
  }

  @Test
  fun merge_attachesBinaryRendererToConflictingBinaryField() {
    val local = ProtectedBinary(false, byteArrayOf(1))
    val cloud = ProtectedBinary(false, byteArrayOf(2))
    val diff = emptyEntryDiff().copy(
      binaries = mapOf("same-name.bin" to ThreeWay.Conflict(local, cloud))
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(ConflictRenderer.BINARY, result.conflicts.single().renderer)
  }

  @Test
  fun merge_collectsConflictingStandardIconForUserDecision() {
    val local = PwIconStandard(1)
    val cloud = PwIconStandard(2)
    val diff = emptyEntryDiff().copy(
      icon = ThreeWay.Conflict(local, cloud)
    )

    val result = AutoMergerImpl().merge(diff)

    assertFalse(result.autoResolved.containsKey(FieldKey.Icon))
    assertEquals(
      listOf(
        FieldConflict(
          key = FieldKey.Icon,
          local = FieldValue.IconValue(local),
          cloud = FieldValue.IconValue(cloud),
          renderer = ConflictRenderer.ICON
        )
      ),
      result.conflicts
    )
  }

  @Test
  fun merge_collectsConflictingCustomIconForUserDecision() {
    val local = PwIconCustom(UUID(0, 1), byteArrayOf(1))
    val cloud = PwIconCustom(UUID(0, 2), byteArrayOf(2))
    val diff = emptyEntryDiff().copy(
      customIcon = ThreeWay.Conflict(local, cloud)
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      listOf(
        FieldConflict(
          key = FieldKey.CustomIcon,
          local = FieldValue.CustomIconValue(local),
          cloud = FieldValue.CustomIconValue(cloud),
          renderer = ConflictRenderer.CUSTOM_ICON
        )
      ),
      result.conflicts
    )
  }

  @Test
  fun merge_collectsConflictingExpireDateForUserDecision() {
    val local = Date(1_000)
    val cloud = Date(2_000)
    val diff = emptyEntryDiff().copy(
      expireDate = ThreeWay.Conflict(local, cloud)
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldConflict(
        key = FieldKey.ExpireDate,
        local = FieldValue.NullableDateValue(local),
        cloud = FieldValue.NullableDateValue(cloud),
        renderer = ConflictRenderer.DATE
      ),
      result.conflicts.single()
    )
  }

  @Test
  fun merge_collectsConflictingExpiresForUserDecision() {
    val diff = emptyEntryDiff().copy(
      expires = ThreeWay.Conflict(false, true)
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldConflict(
        key = FieldKey.Expires,
        local = FieldValue.BooleanValue(false),
        cloud = FieldValue.BooleanValue(true),
        renderer = ConflictRenderer.BOOLEAN
      ),
      result.conflicts.single()
    )
  }

  @Test
  fun merge_collectsConflictingGroupNotesForUserDecision() {
    val diff = emptyGroupDiff().copy(
      notes = ThreeWay.Conflict("local notes", "cloud notes")
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldConflict(
        key = FieldKey.GroupNotes,
        local = FieldValue.TextValue("local notes"),
        cloud = FieldValue.TextValue("cloud notes")
      ),
      result.conflicts.single()
    )
  }

  @Test
  fun merge_collectsConflictingPwGroupV4PropertyForUserDecision() {
    val diff = emptyGroupDiff().copy(
      properties = mapOf(
        GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE to ThreeWay.Conflict(
          local = EntryPropertyValue.Text("local-sequence"),
          cloud = EntryPropertyValue.Text("cloud-sequence")
        )
      )
    )

    val result = AutoMergerImpl().merge(diff)

    assertFalse(result.autoResolved.containsKey(FieldKey.GroupPropertyField(GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE)))
    assertEquals(
      FieldConflict(
        key = FieldKey.GroupPropertyField(GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE),
        local = FieldValue.PropertyValue(EntryPropertyValue.Text("local-sequence")),
        cloud = FieldValue.PropertyValue(EntryPropertyValue.Text("cloud-sequence")),
        renderer = ConflictRenderer.GROUP_PROPERTY
      ),
      result.conflicts.single()
    )
  }

  @Test
  fun merge_autoResolvesWhitelistedGroupPropertiesWithCloudValues() {
    val properties = linkedMapOf(
      GroupProperty.LAST_MODIFICATION_TIME to ThreeWay.Conflict(
        EntryPropertyValue.DateTime(java.util.Date(1)),
        EntryPropertyValue.DateTime(java.util.Date(2))
      ),
      GroupProperty.LAST_ACCESS_TIME to ThreeWay.Conflict(
        EntryPropertyValue.DateTime(java.util.Date(3)),
        EntryPropertyValue.DateTime(java.util.Date(4))
      ),
      GroupProperty.USAGE_COUNT to ThreeWay.Conflict(
        EntryPropertyValue.LongNumber(5),
        EntryPropertyValue.LongNumber(6)
      )
    )

    val result = AutoMergerImpl().merge(emptyGroupDiff().copy(properties = properties))

    properties.forEach { (property, difference) ->
      val cloudValue = (difference as ThreeWay.Conflict).cloud
      assertEquals(
        FieldValue.PropertyValue(cloudValue),
        result.autoResolved[FieldKey.GroupPropertyField(property)]
      )
    }
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesConflictingGroupTagsBySemicolonUnion() {
    val diff = emptyGroupDiff().copy(
      tags = ThreeWay.Conflict(
        local = "alpha;beta",
        cloud = "beta;gamma"
      )
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.TagsValue("alpha;beta;gamma"),
      result.autoResolved[FieldKey.Tags]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun merge_autoResolvesConflictingTagsBySemicolonUnion() {
    val diff = emptyEntryDiff().copy(
      tags = ThreeWay.Conflict(
        local = "alpha;beta",
        cloud = "beta;gamma"
      )
    )

    val result = AutoMergerImpl().merge(diff)

    assertEquals(
      FieldValue.TagsValue("alpha;beta;gamma"),
      result.autoResolved[FieldKey.Tags]
    )
    assertTrue(result.conflicts.isEmpty())
  }

  private fun emptyEntryDiff(): EntryDiff {
    return EntryDiff(
      strings = emptyMap(),
      binaries = emptyMap(),
      properties = emptyMap(),
      icon = ThreeWay.Same(PwIconStandard(0)),
      customIcon = ThreeWay.Same(null),
      tags = ThreeWay.Same(""),
      expireDate = ThreeWay.Same(null),
      expires = ThreeWay.Same(false)
    )
  }

  private fun emptyGroupDiff(): GroupDiff {
    return GroupDiff(
      name = ThreeWay.Same(""),
      notes = ThreeWay.Same(""),
      properties = emptyMap(),
      icon = ThreeWay.Same(PwIconStandard(0)),
      customIcon = ThreeWay.Same(null),
      tags = ThreeWay.Same(""),
      expireDate = ThreeWay.Same(null),
      expires = ThreeWay.Same(false)
    )
  }
}
