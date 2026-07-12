package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV3

object LegacyItemMerger {

  fun differs(local: PwDataInf, cloud: PwDataInf): Boolean {
    require(local::class == cloud::class) { "Legacy compare requires matching database item types" }
    return when {
      local is PwEntryV3 && cloud is PwEntryV3 -> entryDiffers(local, cloud)
      local is PwGroupV3 && cloud is PwGroupV3 -> groupDiffers(local, cloud)
      else -> local != cloud
    }
  }

  fun conflict(local: PwDataInf, cloud: PwDataInf): AutoMergeResult {
    require(local::class == cloud::class) { "Legacy merge requires matching database item types" }
    return AutoMergeResult(
      autoResolved = emptyMap(),
      conflicts = listOf(
        FieldConflict(
          key = FieldKey.LegacyItem,
          local = FieldValue.LegacyItemValue(local),
          cloud = FieldValue.LegacyItemValue(cloud),
          renderer = ConflictRenderer.LEGACY_ITEM
        )
      )
    )
  }

  fun apply(local: PwDataInf, cloud: PwDataInf, decision: Decision): PwDataInf {
    require(local::class == cloud::class) { "Legacy merge requires matching database item types" }
    val selected = if (decision == Decision.LOCAL) local else cloud
    return when {
      local is PwEntry && selected is PwEntry -> copyEntry(local, selected)
      local is PwGroup && selected is PwGroup -> copyGroup(local, selected)
      else -> throw IllegalArgumentException("Unsupported legacy merge item: ${local::class.java.name}")
    }
  }

  private fun copyEntry(local: PwEntry, selected: PwEntry): PwEntry {
    val result = selected.clone() as PwEntry
    result.setUUID(local.uuid)
    local.parent?.let { result.setParent(it) }
    return result
  }

  private fun copyGroup(local: PwGroup, selected: PwGroup): PwGroup {
    val result = selected.clone()
    result.setId(local.id)
    result.childGroups = local.childGroups
    result.childEntries = local.childEntries
    local.parent?.let { result.setParent(it) }
    return result
  }

  private fun entryDiffers(local: PwEntryV3, cloud: PwEntryV3): Boolean {
    return local.icon != cloud.icon ||
        local.username != cloud.username ||
        !sameBytes(local.passwordBytes, cloud.passwordBytes) ||
        local.title != cloud.title ||
        local.url != cloud.url ||
        local.additional != cloud.additional ||
        local.creationTime != cloud.creationTime ||
        local.lastModificationTime != cloud.lastModificationTime ||
        local.lastAccessTime != cloud.lastAccessTime ||
        local.expiryTime != cloud.expiryTime ||
        local.binaryDesc != cloud.binaryDesc ||
        !sameBytes(local.binaryData, cloud.binaryData)
  }

  private fun groupDiffers(local: PwGroupV3, cloud: PwGroupV3): Boolean {
    return local.name != cloud.name ||
        local.icon != cloud.icon ||
        local.tCreation != cloud.tCreation ||
        local.tLastMod != cloud.tLastMod ||
        local.tLastAccess != cloud.tLastAccess ||
        local.tExpire != cloud.tExpire ||
        local.flags != cloud.flags
  }

  private fun sameBytes(local: ByteArray?, cloud: ByteArray?): Boolean {
    return when {
      local == null -> cloud == null
      cloud == null -> false
      else -> local.contentEquals(cloud)
    }
  }
}
