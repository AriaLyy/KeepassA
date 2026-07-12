package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import java.util.Date

interface AutoMerger {
  fun merge(diff: EntryDiff): AutoMergeResult
  fun merge(diff: GroupDiff): AutoMergeResult
}

class AutoMergerImpl : AutoMerger {
  override fun merge(diff: EntryDiff): AutoMergeResult {
    val autoResolved = linkedMapOf<FieldKey, FieldValue>()
    val conflicts = mutableListOf<FieldConflict>()

    diff.strings.forEach { (key, value) ->
      val fieldKey = FieldKey.StringField(key)
      when (value) {
        is ThreeWay.Same -> autoResolved[fieldKey] = FieldValue.StringValue(value.value)
        is ThreeWay.Conflict -> conflicts += FieldConflict(
          key = fieldKey,
          local = FieldValue.StringValue(value.local),
          cloud = FieldValue.StringValue(value.cloud),
          renderer = ConflictRenderer.PROTECTED_STRING
        )
        is ThreeWay.OnlyLocal -> autoResolved[fieldKey] = FieldValue.StringValue(value.value)
        is ThreeWay.OnlyCloud -> autoResolved[fieldKey] = FieldValue.StringValue(value.value)
      }
    }
    diff.binaries.forEach { (key, value) ->
      val fieldKey = FieldKey.BinaryField(key)
      when (value) {
        is ThreeWay.Same -> autoResolved[fieldKey] = FieldValue.BinaryValue(value.value, key)
        is ThreeWay.Conflict -> conflicts += FieldConflict(
          key = fieldKey,
          local = FieldValue.BinaryValue(value.local, key),
          cloud = FieldValue.BinaryValue(value.cloud, key),
          renderer = ConflictRenderer.BINARY
        )
        is ThreeWay.OnlyLocal -> autoResolved[fieldKey] = FieldValue.BinaryValue(value.value, key)
        is ThreeWay.OnlyCloud -> autoResolved[fieldKey] = FieldValue.BinaryValue(value.value, key)
      }
    }
    diff.properties.forEach { (property, value) ->
      val fieldKey = FieldKey.EntryPropertyField(property)
      when (value) {
        is ThreeWay.Same -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
        is ThreeWay.Conflict -> if (property in CLOUD_ENTRY_PROPERTY_WHITELIST) {
          autoResolved[fieldKey] = FieldValue.PropertyValue(value.cloud)
        } else {
          conflicts += FieldConflict(
            key = fieldKey,
            local = FieldValue.PropertyValue(value.local),
            cloud = FieldValue.PropertyValue(value.cloud),
            renderer = ConflictRenderer.ENTRY_PROPERTY
          )
        }
        is ThreeWay.OnlyLocal -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
        is ThreeWay.OnlyCloud -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
      }
    }
    when (val icon = diff.icon) {
      is ThreeWay.Same -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.Icon,
        local = FieldValue.IconValue(icon.local),
        cloud = FieldValue.IconValue(icon.cloud),
        renderer = ConflictRenderer.ICON
      )
      is ThreeWay.OnlyLocal -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
      is ThreeWay.OnlyCloud -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
    }
    when (val customIcon = diff.customIcon) {
      is ThreeWay.Same -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.CustomIcon,
        local = FieldValue.CustomIconValue(customIcon.local),
        cloud = FieldValue.CustomIconValue(customIcon.cloud),
        renderer = ConflictRenderer.CUSTOM_ICON
      )
      is ThreeWay.OnlyLocal -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
      is ThreeWay.OnlyCloud -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
    }
    val mergedTags = mergeTags(diff.tags)
    if (mergedTags.isNotEmpty()) {
      autoResolved[FieldKey.Tags] = FieldValue.TagsValue(mergedTags)
    }
    when (val expireDate = diff.expireDate) {
      is ThreeWay.Same -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.ExpireDate,
        local = FieldValue.NullableDateValue(expireDate.local),
        cloud = FieldValue.NullableDateValue(expireDate.cloud),
        renderer = ConflictRenderer.DATE
      )
      is ThreeWay.OnlyLocal -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
      is ThreeWay.OnlyCloud -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
    }
    when (val expires = diff.expires) {
      is ThreeWay.Same -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.Expires,
        local = FieldValue.BooleanValue(expires.local),
        cloud = FieldValue.BooleanValue(expires.cloud),
        renderer = ConflictRenderer.BOOLEAN
      )
      is ThreeWay.OnlyLocal -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
      is ThreeWay.OnlyCloud -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
    }

    return AutoMergeResult(
      autoResolved = autoResolved,
      conflicts = conflicts
    )
  }

  override fun merge(diff: GroupDiff): AutoMergeResult {
    val autoResolved = linkedMapOf<FieldKey, FieldValue>()
    val conflicts = mutableListOf<FieldConflict>()

    mergeText(FieldKey.GroupName, diff.name, autoResolved, conflicts)
    mergeText(FieldKey.GroupNotes, diff.notes, autoResolved, conflicts)
    diff.properties.forEach { (property, value) ->
      val fieldKey = FieldKey.GroupPropertyField(property)
      when (value) {
        is ThreeWay.Same -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
        is ThreeWay.Conflict -> if (property in CLOUD_GROUP_PROPERTY_WHITELIST) {
          autoResolved[fieldKey] = FieldValue.PropertyValue(value.cloud)
        } else {
          conflicts += FieldConflict(
            key = fieldKey,
            local = FieldValue.PropertyValue(value.local),
            cloud = FieldValue.PropertyValue(value.cloud),
            renderer = ConflictRenderer.GROUP_PROPERTY
          )
        }
        is ThreeWay.OnlyLocal -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
        is ThreeWay.OnlyCloud -> autoResolved[fieldKey] = FieldValue.PropertyValue(value.value)
      }
    }
    mergeIcon(diff.icon, autoResolved, conflicts)
    mergeCustomIcon(diff.customIcon, autoResolved, conflicts)
    val mergedTags = mergeTags(diff.tags)
    if (mergedTags.isNotEmpty()) {
      autoResolved[FieldKey.Tags] = FieldValue.TagsValue(mergedTags)
    }
    mergeExpireDate(diff.expireDate, autoResolved, conflicts)
    mergeExpires(diff.expires, autoResolved, conflicts)

    return AutoMergeResult(
      autoResolved = autoResolved,
      conflicts = conflicts
    )
  }

  private fun mergeText(
    key: FieldKey,
    value: ThreeWay<String>,
    autoResolved: MutableMap<FieldKey, FieldValue>,
    conflicts: MutableList<FieldConflict>
  ) {
    when (value) {
      is ThreeWay.Same -> autoResolved[key] = FieldValue.TextValue(value.value)
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = key,
        local = FieldValue.TextValue(value.local),
        cloud = FieldValue.TextValue(value.cloud),
        renderer = ConflictRenderer.TEXT
      )
      is ThreeWay.OnlyLocal -> autoResolved[key] = FieldValue.TextValue(value.value)
      is ThreeWay.OnlyCloud -> autoResolved[key] = FieldValue.TextValue(value.value)
    }
  }

  private fun mergeIcon(
    icon: ThreeWay<PwIconStandard>,
    autoResolved: MutableMap<FieldKey, FieldValue>,
    conflicts: MutableList<FieldConflict>
  ) {
    when (icon) {
      is ThreeWay.Same -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.Icon,
        local = FieldValue.IconValue(icon.local),
        cloud = FieldValue.IconValue(icon.cloud),
        renderer = ConflictRenderer.ICON
      )
      is ThreeWay.OnlyLocal -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
      is ThreeWay.OnlyCloud -> autoResolved[FieldKey.Icon] = FieldValue.IconValue(icon.value)
    }
  }

  private fun mergeCustomIcon(
    customIcon: ThreeWay<PwIconCustom?>,
    autoResolved: MutableMap<FieldKey, FieldValue>,
    conflicts: MutableList<FieldConflict>
  ) {
    when (customIcon) {
      is ThreeWay.Same -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.CustomIcon,
        local = FieldValue.CustomIconValue(customIcon.local),
        cloud = FieldValue.CustomIconValue(customIcon.cloud),
        renderer = ConflictRenderer.CUSTOM_ICON
      )
      is ThreeWay.OnlyLocal -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
      is ThreeWay.OnlyCloud -> customIcon.value?.let {
        autoResolved[FieldKey.CustomIcon] = FieldValue.CustomIconValue(it)
      }
    }
  }

  private fun mergeExpireDate(
    expireDate: ThreeWay<Date?>,
    autoResolved: MutableMap<FieldKey, FieldValue>,
    conflicts: MutableList<FieldConflict>
  ) {
    when (expireDate) {
      is ThreeWay.Same -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.ExpireDate,
        local = FieldValue.NullableDateValue(expireDate.local),
        cloud = FieldValue.NullableDateValue(expireDate.cloud),
        renderer = ConflictRenderer.DATE
      )
      is ThreeWay.OnlyLocal -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
      is ThreeWay.OnlyCloud -> expireDate.value?.let {
        autoResolved[FieldKey.ExpireDate] = FieldValue.DateValue(it)
      }
    }
  }

  private fun mergeExpires(
    expires: ThreeWay<Boolean>,
    autoResolved: MutableMap<FieldKey, FieldValue>,
    conflicts: MutableList<FieldConflict>
  ) {
    when (expires) {
      is ThreeWay.Same -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
      is ThreeWay.Conflict -> conflicts += FieldConflict(
        key = FieldKey.Expires,
        local = FieldValue.BooleanValue(expires.local),
        cloud = FieldValue.BooleanValue(expires.cloud),
        renderer = ConflictRenderer.BOOLEAN
      )
      is ThreeWay.OnlyLocal -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
      is ThreeWay.OnlyCloud -> autoResolved[FieldKey.Expires] = FieldValue.BooleanValue(expires.value)
    }
  }

  private fun mergeTags(tags: ThreeWay<String>): String {
    return when (tags) {
      is ThreeWay.Same -> tags.value
      is ThreeWay.OnlyLocal -> tags.value
      is ThreeWay.OnlyCloud -> tags.value
      is ThreeWay.Conflict -> {
        val merged = linkedSetOf<String>()
        merged += splitTags(tags.local)
        merged += splitTags(tags.cloud)
        merged.joinToString(";")
      }
    }
  }

  private fun splitTags(tags: String): List<String> {
    return tags.split(";")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  }

  private companion object {
    val CLOUD_ENTRY_PROPERTY_WHITELIST = setOf(
      EntryProperty.LAST_MODIFICATION_TIME,
      EntryProperty.LAST_ACCESS_TIME,
      EntryProperty.USAGE_COUNT
    )

    val CLOUD_GROUP_PROPERTY_WHITELIST = setOf(
      GroupProperty.LAST_MODIFICATION_TIME,
      GroupProperty.LAST_ACCESS_TIME,
      GroupProperty.USAGE_COUNT
    )
  }
}

sealed class FieldKey {
  data class StringField(val key: String) : FieldKey()
  data class BinaryField(val key: String) : FieldKey()
  data class EntryPropertyField(val property: EntryProperty) : FieldKey()
  data class GroupPropertyField(val property: GroupProperty) : FieldKey()
  object GroupName : FieldKey()
  object GroupNotes : FieldKey()
  object Icon : FieldKey()
  object CustomIcon : FieldKey()
  object Tags : FieldKey()
  object ExpireDate : FieldKey()
  object Expires : FieldKey()
  object LegacyItem : FieldKey()
}

sealed class FieldValue {
  data class StringValue(val value: ProtectedString) : FieldValue()
  data class BinaryValue(val value: ProtectedBinary, val name: String = "") : FieldValue()
  data class TextValue(val value: String) : FieldValue()
  data class IconValue(val value: PwIconStandard) : FieldValue()
  data class CustomIconValue(val value: PwIconCustom?) : FieldValue()
  data class TagsValue(val value: String) : FieldValue()
  data class DateValue(val value: Date) : FieldValue()
  data class NullableDateValue(val value: Date?) : FieldValue()
  data class BooleanValue(val value: Boolean) : FieldValue()
  data class PropertyValue(val value: EntryPropertyValue) : FieldValue()
  data class LegacyItemValue(val value: PwDataInf) : FieldValue()
}

data class AutoMergeResult(
  val autoResolved: Map<FieldKey, FieldValue>,
  val conflicts: List<FieldConflict>
)

enum class ConflictRenderer {
  PROTECTED_STRING,
  BINARY,
  TEXT,
  ICON,
  CUSTOM_ICON,
  DATE,
  BOOLEAN,
  ENTRY_PROPERTY,
  GROUP_PROPERTY,
  LEGACY_ITEM
}

data class FieldConflict(
  val key: FieldKey,
  val local: FieldValue,
  val cloud: FieldValue,
  val renderer: ConflictRenderer = ConflictRenderer.TEXT
)
