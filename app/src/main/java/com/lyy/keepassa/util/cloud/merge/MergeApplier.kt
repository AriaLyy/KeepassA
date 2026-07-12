package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import java.util.Date

interface MergeApplier {
  fun apply(
    local: PwEntry,
    autoMerge: AutoMergeResult,
    decisions: Map<FieldKey, Decision>,
    database: PwDatabaseV4? = null
  ): PwEntry

  fun apply(
    local: PwGroup,
    autoMerge: AutoMergeResult,
    decisions: Map<FieldKey, Decision>,
    database: PwDatabaseV4? = null
  ): PwGroup
}

class MergeApplierImpl : MergeApplier {
  override fun apply(
    local: PwEntry,
    autoMerge: AutoMergeResult,
    decisions: Map<FieldKey, Decision>,
    database: PwDatabaseV4?
  ): PwEntry {
    val result = local.clone(true)

    if (result is PwEntryV4) {
      result.history = ArrayList(result.history)
      result.createBackup(database)
      result.strings = HashMap(result.strings)
      result.binaries = HashMap(result.binaries)
      result.autoType = result.autoType.clone() as PwEntryV4.AutoType

      autoMerge.autoResolved.forEach { (key, value) ->
        if (key is FieldKey.StringField && value is FieldValue.StringValue) {
          result.strings[key.key] = value.value
        }
        if (key is FieldKey.BinaryField && value is FieldValue.BinaryValue) {
          result.binaries[key.key] = value.value
        }
        if (key is FieldKey.EntryPropertyField && value is FieldValue.PropertyValue) {
          result.applyEntryProperty(key.property, value.value)
        }
        if (key is FieldKey.Icon && value is FieldValue.IconValue) {
          result.icon = value.value
        }
        if (key is FieldKey.CustomIcon && value is FieldValue.CustomIconValue) {
          result.customIcon = value.value
        }
        if (key is FieldKey.Tags && value is FieldValue.TagsValue) {
          result.tags = value.value
        }
        if (key is FieldKey.ExpireDate && value is FieldValue.DateValue) {
          result.expiryTime = value.value
        }
        if (key is FieldKey.ExpireDate && value is FieldValue.NullableDateValue) {
          result.expiryTime = value.value
        }
        if (key is FieldKey.Expires && value is FieldValue.BooleanValue) {
          result.setExpiresFlag(value.value)
        }
      }
      autoMerge.conflicts.forEach { conflict ->
        val decision = decisions[conflict.key]
          ?: throw IllegalArgumentException("Missing merge decision for ${conflict.key}")
        val value = when (decision) {
          Decision.LOCAL -> conflict.local
          Decision.CLOUD -> conflict.cloud
        }
        if (conflict.key is FieldKey.StringField && value is FieldValue.StringValue) {
          result.strings[conflict.key.key] = value.value
        }
        if (conflict.key is FieldKey.BinaryField && value is FieldValue.BinaryValue) {
          result.binaries[conflict.key.key] = value.value
        }
        if (conflict.key is FieldKey.EntryPropertyField && value is FieldValue.PropertyValue) {
          result.applyEntryProperty(conflict.key.property, value.value)
        }
        if (conflict.key is FieldKey.Icon && value is FieldValue.IconValue) {
          result.icon = value.value
        }
        if (conflict.key is FieldKey.CustomIcon && value is FieldValue.CustomIconValue) {
          result.customIcon = value.value
        }
        if (conflict.key is FieldKey.ExpireDate && value is FieldValue.DateValue) {
          result.expiryTime = value.value
        }
        if (conflict.key is FieldKey.ExpireDate && value is FieldValue.NullableDateValue) {
          result.expiryTime = value.value
        }
        if (conflict.key is FieldKey.Expires && value is FieldValue.BooleanValue) {
          result.setExpiresFlag(value.value)
        }
      }
      database?.let { target ->
        result.binaries.values.forEach(target.binPool::poolAdd)
        result.customIcon?.takeIf { it.uuid != com.keepassdroid.database.PwIconCustom.ZERO.uuid && it.imageData?.isNotEmpty() == true }
          ?.let { icon -> if (icon !in target.customIcons) target.customIcons.add(icon) }
      }
    }

    return result
  }

  override fun apply(
    local: PwGroup,
    autoMerge: AutoMergeResult,
    decisions: Map<FieldKey, Decision>,
    database: PwDatabaseV4?
  ): PwGroup {
    val result = local.clone()

    autoMerge.autoResolved.forEach { (key, value) ->
      result.applyGroupField(key, value)
    }
    autoMerge.conflicts.forEach { conflict ->
      val value = when (
        decisions[conflict.key]
          ?: throw IllegalArgumentException("Missing merge decision for ${conflict.key}")
      ) {
        Decision.LOCAL -> conflict.local
        Decision.CLOUD -> conflict.cloud
      }
      result.applyGroupField(conflict.key, value)
    }
    (result as? PwGroupV4)?.customIcon
      ?.takeIf { it.uuid != PwDatabaseV4.UUID_ZERO && it.imageData?.isNotEmpty() == true }
      ?.let { icon -> if (database != null && icon !in database.customIcons) database.customIcons.add(icon) }

    return result
  }

  private fun PwGroup.applyGroupField(key: FieldKey, value: FieldValue) {
    if (key is FieldKey.GroupName && value is FieldValue.TextValue) {
      name = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.GroupNotes && value is FieldValue.TextValue) {
      notes = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.GroupPropertyField && value is FieldValue.PropertyValue) {
      applyGroupProperty(key.property, value.value)
    }
    if (key is FieldKey.Icon && value is FieldValue.IconValue) {
      icon = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.CustomIcon && value is FieldValue.CustomIconValue) {
      customIcon = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.Tags && value is FieldValue.TagsValue) {
      tags = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.ExpireDate && value is FieldValue.DateValue) {
      expiryTime = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.ExpireDate && value is FieldValue.NullableDateValue) {
      expiryTime = value.value
    }
    if (this is PwGroupV4 && key is FieldKey.Expires && value is FieldValue.BooleanValue) {
      setExpires(value.value)
    }
  }

  private fun PwEntry.setExpiresFlag(expires: Boolean) {
    setExpires(expires)
  }

  private fun PwEntryV4.applyEntryProperty(property: EntryProperty, value: EntryPropertyValue) {
    when (property) {
      EntryProperty.FOREGROUND_COLOR -> foregroundColor = (value as EntryPropertyValue.Text).value
      EntryProperty.BACKGROUND_COLOR -> backgroupColor = (value as EntryPropertyValue.Text).value
      EntryProperty.OVERRIDE_URL -> overrideURL = (value as EntryPropertyValue.Text).value
      EntryProperty.AUTO_TYPE -> applyAutoType(value as EntryPropertyValue.AutoType)
      EntryProperty.LOCATION_CHANGED -> setLocationChanged((value as EntryPropertyValue.DateTime).value)
      EntryProperty.CREATION_TIME -> setCreationTime((value as EntryPropertyValue.DateTime).value)
      EntryProperty.LAST_MODIFICATION_TIME -> setLastModificationTime((value as EntryPropertyValue.DateTime).value)
      EntryProperty.LAST_ACCESS_TIME -> setLastAccessTime((value as EntryPropertyValue.DateTime).value)
      EntryProperty.USAGE_COUNT -> setUsageCount((value as EntryPropertyValue.LongNumber).value)
      EntryProperty.INTERNAL_URL -> setPrivateUrl((value as EntryPropertyValue.Text).value)
      EntryProperty.ADDITIONAL -> additional = (value as EntryPropertyValue.Text).value
      EntryProperty.CUSTOM_DATA -> customData = (value as EntryPropertyValue.CustomData).toPwCustomData()
      EntryProperty.PREVIOUS_PARENT_GROUP -> prevParentGroup = (value as EntryPropertyValue.UuidValue).value
      EntryProperty.QUALITY_CHECK -> qualityCheck = (value as EntryPropertyValue.BooleanFlag).value
    }
  }

  private fun PwGroupV4.applyGroupProperty(property: GroupProperty, value: EntryPropertyValue) {
    when (property) {
      GroupProperty.IS_EXPANDED -> isExpanded = (value as EntryPropertyValue.BooleanFlag).value
      GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE -> defaultAutoTypeSequence = (value as EntryPropertyValue.Text).value
      GroupProperty.ENABLE_AUTO_TYPE -> enableAutoType = (value as EntryPropertyValue.NullableBooleanFlag).value
      GroupProperty.ENABLE_SEARCHING -> enableSearching = (value as EntryPropertyValue.NullableBooleanFlag).value
      GroupProperty.LAST_TOP_VISIBLE_ENTRY -> lastTopVisibleEntry = (value as EntryPropertyValue.UuidValue).value
      GroupProperty.LOCATION_CHANGED -> setLocationChanged((value as EntryPropertyValue.DateTime).value)
      GroupProperty.CREATION_TIME -> setCreationTime((value as EntryPropertyValue.DateTime).value)
      GroupProperty.LAST_MODIFICATION_TIME -> setLastModificationTime((value as EntryPropertyValue.DateTime).value)
      GroupProperty.LAST_ACCESS_TIME -> setLastAccessTime((value as EntryPropertyValue.DateTime).value)
      GroupProperty.USAGE_COUNT -> setUsageCount((value as EntryPropertyValue.LongNumber).value)
      GroupProperty.PREVIOUS_PARENT_GROUP -> prevParentGroup = (value as EntryPropertyValue.UuidValue).value
      GroupProperty.CUSTOM_DATA -> customData = (value as EntryPropertyValue.CustomData).toPwCustomData()
    }
  }

  private fun PwEntryV4.applyAutoType(value: EntryPropertyValue.AutoType) {
    autoType.enabled = value.enabled
    autoType.obfuscationOptions = value.obfuscationOptions
    autoType.defaultSequence = value.defaultSequence
    autoType.setWindowSequencePairs(value.windowSequencePairs)
  }

  private fun PwEntryV4.AutoType.setWindowSequencePairs(values: Map<String, String>) {
    val field = javaClass.getDeclaredField("windowSeqPairs")
    field.isAccessible = true
    field.set(this, HashMap(values))
  }

  private fun EntryPropertyValue.CustomData.toPwCustomData(): PwCustomData {
    return PwCustomData().also { data ->
      data.putAll(values)
      data.lastMod = lastModified.mapValues { (_, value) -> value.copyDate() }.toMutableMap()
    }
  }

  private fun PwEntryV4.setPrivateUrl(value: String?) {
    val field = generateSequence(javaClass as Class<*>) { it.superclass }
      .mapNotNull { type ->
        runCatching { type.getDeclaredField("url") }.getOrNull()
      }
      .first()
    field.isAccessible = true
    field.set(this, value)
  }

  private fun Date.copyDate(): Date {
    return Date(time)
  }
}

enum class Decision {
  LOCAL,
  CLOUD
}
