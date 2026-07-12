package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import java.util.Date
import java.util.UUID

interface EntryDiffer {
  fun diff(local: PwEntry, cloud: PwEntry): EntryDiff
  fun diff(local: PwGroup, cloud: PwGroup): GroupDiff
}

class EntryDifferImpl : EntryDiffer {
  override fun diff(local: PwEntry, cloud: PwEntry): EntryDiff {
    val localStrings = (local as? PwEntryV4)?.strings.orEmpty()
    val cloudStrings = (cloud as? PwEntryV4)?.strings.orEmpty()
    val localBinaries = (local as? PwEntryV4)?.binaries.orEmpty()
    val cloudBinaries = (cloud as? PwEntryV4)?.binaries.orEmpty()
    val localV4 = local as? PwEntryV4
    val cloudV4 = cloud as? PwEntryV4

    return EntryDiff(
      strings = diffMap(localStrings, cloudStrings),
      binaries = diffBinaries(localBinaries, cloudBinaries),
      properties = if (localV4 != null && cloudV4 != null) {
        diffEntryProperties(localV4, cloudV4)
      } else {
        emptyMap()
      },
      icon = diffValue(local.icon, cloud.icon),
      customIcon = diffValue(localV4?.customIcon, cloudV4?.customIcon),
      tags = diffValue(localV4?.tags.orEmpty(), cloudV4?.tags.orEmpty()),
      expireDate = diffValue(local.expiryTime, cloud.expiryTime),
      expires = diffValue(local.expires(), cloud.expires())
    )
  }

  override fun diff(local: PwGroup, cloud: PwGroup): GroupDiff {
    val localV4 = local as? PwGroupV4
    val cloudV4 = cloud as? PwGroupV4
    return GroupDiff(
      name = diffValue(local.name.orEmpty(), cloud.name.orEmpty()),
      notes = diffValue(localV4?.notes.orEmpty(), cloudV4?.notes.orEmpty()),
      properties = if (localV4 != null && cloudV4 != null) {
        diffGroupProperties(localV4, cloudV4)
      } else {
        emptyMap()
      },
      icon = diffValue(local.icon, cloud.icon),
      customIcon = diffValue(localV4?.customIcon, cloudV4?.customIcon),
      tags = diffValue(localV4?.tags.orEmpty(), cloudV4?.tags.orEmpty()),
      expireDate = diffValue(localV4?.expiryTime, cloudV4?.expiryTime),
      expires = diffValue(localV4?.expires() ?: false, cloudV4?.expires() ?: false)
    )
  }

  private fun <T> diffMap(
    local: Map<String, T>,
    cloud: Map<String, T>,
    sameValue: (T, T) -> Boolean = { left, right -> left == right }
  ): Map<String, ThreeWay<T>> {
    return (local.keys + cloud.keys).associateWith { key ->
      val localValue = local[key]
      val cloudValue = cloud[key]
      when {
        localValue != null && cloudValue == null -> ThreeWay.OnlyLocal(localValue)
        localValue != null && sameValue(localValue, cloudValue!!) -> ThreeWay.Same(localValue)
        localValue != null -> ThreeWay.Conflict(localValue, cloudValue!!)
        else -> ThreeWay.OnlyCloud(cloudValue!!)
      }
    }
  }

  private fun diffBinaries(
    local: Map<String, ProtectedBinary>,
    cloud: Map<String, ProtectedBinary>
  ): Map<String, ThreeWay<ProtectedBinary>> = diffMap(local, cloud, ::sameBinary)

  private fun diffEntryProperties(
    local: PwEntryV4,
    cloud: PwEntryV4
  ): Map<EntryProperty, ThreeWay<EntryPropertyValue>> {
    return linkedMapOf(
      EntryProperty.FOREGROUND_COLOR to diffValue(
        EntryPropertyValue.Text(local.foregroundColor),
        EntryPropertyValue.Text(cloud.foregroundColor)
      ),
      EntryProperty.BACKGROUND_COLOR to diffValue(
        EntryPropertyValue.Text(local.backgroupColor),
        EntryPropertyValue.Text(cloud.backgroupColor)
      ),
      EntryProperty.OVERRIDE_URL to diffValue(
        EntryPropertyValue.Text(local.overrideURL),
        EntryPropertyValue.Text(cloud.overrideURL)
      ),
      EntryProperty.AUTO_TYPE to diffValue(
        local.autoType.toEntryPropertyValue(),
        cloud.autoType.toEntryPropertyValue()
      ),
      EntryProperty.LOCATION_CHANGED to diffValue(
        EntryPropertyValue.DateTime(local.locationChanged?.copyDate()),
        EntryPropertyValue.DateTime(cloud.locationChanged?.copyDate())
      ),
      EntryProperty.CREATION_TIME to diffValue(
        EntryPropertyValue.DateTime(local.creationTime?.copyDate()),
        EntryPropertyValue.DateTime(cloud.creationTime?.copyDate())
      ),
      EntryProperty.LAST_MODIFICATION_TIME to diffValue(
        EntryPropertyValue.DateTime(local.lastModificationTime?.copyDate()),
        EntryPropertyValue.DateTime(cloud.lastModificationTime?.copyDate())
      ),
      EntryProperty.LAST_ACCESS_TIME to diffValue(
        EntryPropertyValue.DateTime(local.lastAccessTime?.copyDate()),
        EntryPropertyValue.DateTime(cloud.lastAccessTime?.copyDate())
      ),
      EntryProperty.USAGE_COUNT to diffValue(
        EntryPropertyValue.LongNumber(local.usageCount),
        EntryPropertyValue.LongNumber(cloud.usageCount)
      ),
      EntryProperty.INTERNAL_URL to diffValue(
        EntryPropertyValue.Text(local.privateUrl()),
        EntryPropertyValue.Text(cloud.privateUrl())
      ),
      EntryProperty.ADDITIONAL to diffValue(
        EntryPropertyValue.Text(local.additional),
        EntryPropertyValue.Text(cloud.additional)
      ),
      EntryProperty.CUSTOM_DATA to diffValue(
        local.customData.toEntryPropertyValue(),
        cloud.customData.toEntryPropertyValue()
      ),
      EntryProperty.PREVIOUS_PARENT_GROUP to diffValue(
        EntryPropertyValue.UuidValue(local.prevParentGroup),
        EntryPropertyValue.UuidValue(cloud.prevParentGroup)
      ),
      EntryProperty.QUALITY_CHECK to diffValue(
        EntryPropertyValue.BooleanFlag(local.qualityCheck),
        EntryPropertyValue.BooleanFlag(cloud.qualityCheck)
      )
    )
  }

  private fun diffGroupProperties(
    local: PwGroupV4,
    cloud: PwGroupV4
  ): Map<GroupProperty, ThreeWay<EntryPropertyValue>> {
    return linkedMapOf(
      GroupProperty.IS_EXPANDED to diffValue(
        EntryPropertyValue.BooleanFlag(local.isExpanded),
        EntryPropertyValue.BooleanFlag(cloud.isExpanded)
      ),
      GroupProperty.DEFAULT_AUTO_TYPE_SEQUENCE to diffValue(
        EntryPropertyValue.Text(local.defaultAutoTypeSequence),
        EntryPropertyValue.Text(cloud.defaultAutoTypeSequence)
      ),
      GroupProperty.ENABLE_AUTO_TYPE to diffValue(
        EntryPropertyValue.NullableBooleanFlag(local.enableAutoType),
        EntryPropertyValue.NullableBooleanFlag(cloud.enableAutoType)
      ),
      GroupProperty.ENABLE_SEARCHING to diffValue(
        EntryPropertyValue.NullableBooleanFlag(local.enableSearching),
        EntryPropertyValue.NullableBooleanFlag(cloud.enableSearching)
      ),
      GroupProperty.LAST_TOP_VISIBLE_ENTRY to diffValue(
        EntryPropertyValue.UuidValue(local.lastTopVisibleEntry),
        EntryPropertyValue.UuidValue(cloud.lastTopVisibleEntry)
      ),
      GroupProperty.LOCATION_CHANGED to diffValue(
        EntryPropertyValue.DateTime(local.getLocationChanged()?.copyDate()),
        EntryPropertyValue.DateTime(cloud.getLocationChanged()?.copyDate())
      ),
      GroupProperty.CREATION_TIME to diffValue(
        EntryPropertyValue.DateTime(local.getCreationTime()?.copyDate()),
        EntryPropertyValue.DateTime(cloud.getCreationTime()?.copyDate())
      ),
      GroupProperty.LAST_MODIFICATION_TIME to diffValue(
        EntryPropertyValue.DateTime(local.getLastModificationTime()?.copyDate()),
        EntryPropertyValue.DateTime(cloud.getLastModificationTime()?.copyDate())
      ),
      GroupProperty.LAST_ACCESS_TIME to diffValue(
        EntryPropertyValue.DateTime(local.getLastAccessTime()?.copyDate()),
        EntryPropertyValue.DateTime(cloud.getLastAccessTime()?.copyDate())
      ),
      GroupProperty.USAGE_COUNT to diffValue(
        EntryPropertyValue.LongNumber(local.getUsageCount()),
        EntryPropertyValue.LongNumber(cloud.getUsageCount())
      ),
      GroupProperty.PREVIOUS_PARENT_GROUP to diffValue(
        EntryPropertyValue.UuidValue(local.prevParentGroup),
        EntryPropertyValue.UuidValue(cloud.prevParentGroup)
      ),
      GroupProperty.CUSTOM_DATA to diffValue(
        local.customData.toEntryPropertyValue(),
        cloud.customData.toEntryPropertyValue()
      )
    )
  }

  private fun PwEntryV4.AutoType.toEntryPropertyValue(): EntryPropertyValue.AutoType {
    return EntryPropertyValue.AutoType(
      enabled = enabled,
      obfuscationOptions = obfuscationOptions,
      defaultSequence = defaultSequence.orEmpty(),
      windowSequencePairs = entrySet().associate { it.key to it.value }
    )
  }

  private fun PwCustomData?.toEntryPropertyValue(): EntryPropertyValue.CustomData {
    return EntryPropertyValue.CustomData(
      values = this?.entries?.associate { it.key to it.value }.orEmpty(),
      lastModified = this?.lastMod?.entries?.associate { it.key to it.value.copyDate() }.orEmpty()
    )
  }

  private fun PwEntryV4.privateUrl(): String? {
    val field = generateSequence(javaClass as Class<*>) { it.superclass }
      .mapNotNull { type ->
        runCatching { type.getDeclaredField("url") }.getOrNull()
      }
      .firstOrNull() ?: return null
    field.isAccessible = true
    return field.get(this) as? String
  }

  private fun Date.copyDate(): Date {
    return Date(time)
  }

  private val PwEntryV4.locationChanged: Date?
    get() = getLocationChanged()

  private val PwEntryV4.creationTime: Date?
    get() = getCreationTime()

  private val PwEntryV4.lastModificationTime: Date?
    get() = getLastModificationTime()

  private val PwEntryV4.lastAccessTime: Date?
    get() = getLastAccessTime()

  private val PwEntryV4.usageCount: Long
    get() = getUsageCount()

  private fun sameBinary(
    local: ProtectedBinary,
    cloud: ProtectedBinary
  ): Boolean {
    if (local.isProtected != cloud.isProtected) {
      return false
    }
    if (local.length() != cloud.length()) {
      return false
    }
    val localBytes = local.readBytesOrNull() ?: return false
    val cloudBytes = cloud.readBytesOrNull() ?: return false
    return localBytes.contentEquals(cloudBytes)
  }

  private fun ProtectedBinary.readBytesOrNull(): ByteArray? {
    return try {
      val input = getData()
      if (input == null) {
        if (length() == 0) ByteArray(0) else null
      } else {
        input.use { it.readBytes() }
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun <T> diffValue(
    local: T,
    cloud: T
  ): ThreeWay<T> {
    return if (local == cloud) {
      ThreeWay.Same(local)
    } else {
      ThreeWay.Conflict(local, cloud)
    }
  }
}

sealed class ThreeWay<out T> {
  data class Same<T>(val value: T) : ThreeWay<T>()
  data class OnlyLocal<T>(val value: T) : ThreeWay<T>()
  data class OnlyCloud<T>(val value: T) : ThreeWay<T>()
  data class Conflict<T>(val local: T, val cloud: T) : ThreeWay<T>()
}

data class EntryDiff(
  val strings: Map<String, ThreeWay<ProtectedString>>,
  val binaries: Map<String, ThreeWay<ProtectedBinary>>,
  val properties: Map<EntryProperty, ThreeWay<EntryPropertyValue>>,
  val icon: ThreeWay<PwIconStandard>,
  val customIcon: ThreeWay<PwIconCustom?>,
  val tags: ThreeWay<String>,
  val expireDate: ThreeWay<Date?>,
  val expires: ThreeWay<Boolean>
)

enum class EntryProperty {
  FOREGROUND_COLOR,
  BACKGROUND_COLOR,
  OVERRIDE_URL,
  AUTO_TYPE,
  LOCATION_CHANGED,
  CREATION_TIME,
  LAST_MODIFICATION_TIME,
  LAST_ACCESS_TIME,
  USAGE_COUNT,
  INTERNAL_URL,
  ADDITIONAL,
  CUSTOM_DATA,
  PREVIOUS_PARENT_GROUP,
  QUALITY_CHECK
}

enum class GroupProperty {
  IS_EXPANDED,
  DEFAULT_AUTO_TYPE_SEQUENCE,
  ENABLE_AUTO_TYPE,
  ENABLE_SEARCHING,
  LAST_TOP_VISIBLE_ENTRY,
  LOCATION_CHANGED,
  CREATION_TIME,
  LAST_MODIFICATION_TIME,
  LAST_ACCESS_TIME,
  USAGE_COUNT,
  PREVIOUS_PARENT_GROUP,
  CUSTOM_DATA
}

sealed class EntryPropertyValue {
  data class Text(val value: String?) : EntryPropertyValue()
  data class BooleanFlag(val value: Boolean) : EntryPropertyValue()
  data class NullableBooleanFlag(val value: Boolean?) : EntryPropertyValue()
  data class LongNumber(val value: Long) : EntryPropertyValue()
  data class UuidValue(val value: UUID?) : EntryPropertyValue()
  data class DateTime(val value: Date?) : EntryPropertyValue()
  data class AutoType(
    val enabled: Boolean,
    val obfuscationOptions: Long,
    val defaultSequence: String,
    val windowSequencePairs: Map<String, String>
  ) : EntryPropertyValue()

  data class CustomData(
    val values: Map<String, String>,
    val lastModified: Map<String, Date>
  ) : EntryPropertyValue()
}

data class GroupDiff(
  val name: ThreeWay<String>,
  val notes: ThreeWay<String>,
  val properties: Map<GroupProperty, ThreeWay<EntryPropertyValue>>,
  val icon: ThreeWay<PwIconStandard>,
  val customIcon: ThreeWay<PwIconCustom?>,
  val tags: ThreeWay<String>,
  val expireDate: ThreeWay<Date?>,
  val expires: ThreeWay<Boolean>
)
