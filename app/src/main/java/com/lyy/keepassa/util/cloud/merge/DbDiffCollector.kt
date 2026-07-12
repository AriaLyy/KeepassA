package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwGroupV3
import com.lyy.keepassa.util.cloud.PwDataMap

data class DbCompareLists(
  val modifyList: ArrayList<Pair<PwDataInf, PwDataInf>> = arrayListOf(),
  val delList: ArrayList<PwDataInf> = arrayListOf(),
  val newList: ArrayList<PwDataInf> = arrayListOf(),
  val moveList: ArrayList<PwDataMap> = arrayListOf()
)

class DbDiffCollector(
  private val differ: EntryDiffer = EntryDifferImpl()
) {

  fun collect(
    cloudDb: PwDatabase,
    localDb: PwDatabase
  ): DbCompareLists {
    val result = DbCompareLists()

    for (cloudEntry in cloudDb.entries.values) {
      val localEntry = localDb.entries[cloudEntry.uuid]
      if (localEntry == null) {
        result.newList.add(cloudEntry)
        continue
      }
      if (cloudEntry.parent.id != localEntry.parent.id) {
        result.moveList.add(PwDataMap(cloudEntry, localEntry))
      }
      if (entryContentDiffers(localEntry, cloudEntry)) {
        result.modifyList.add(Pair(cloudEntry, localEntry))
      }
    }

    for (cloudGroup in cloudDb.groups.values) {
      val localGroup = localDb.groups[cloudGroup.id]
      if (localGroup == null) {
        result.newList.add(cloudGroup)
        continue
      }
      if (cloudGroup.parent != null && cloudGroup.parent.id != localGroup.parent.id) {
        result.moveList.add(PwDataMap(cloudGroup, localGroup))
      }
      if (groupContentDiffers(localGroup, cloudGroup)) {
        result.modifyList.add(Pair(cloudGroup, localGroup))
      }
    }

    for (localEntry in localDb.entries.values.toList().asReversed()) {
      if (cloudDb.entries[localEntry.uuid] == null) {
        result.delList.add(localEntry)
      }
    }

    for (localGroup in localDb.groups.values.toList().asReversed()) {
      if (localGroup !== localDb.rootGroup && cloudDb.groups[localGroup.id] == null) {
        result.delList.add(localGroup)
      }
    }

    return result
  }

  private fun entryContentDiffers(
    local: PwEntry,
    cloud: PwEntry
  ): Boolean {
    if (local is PwEntryV4 && cloud is PwEntryV4) {
      return differ.diff(local, cloud).hasDifferences()
    }
    if (local is PwEntryV3 && cloud is PwEntryV3) {
      return LegacyItemMerger.differs(local, cloud)
    }
    return cloud != local
  }

  private fun groupContentDiffers(
    local: PwGroup,
    cloud: PwGroup
  ): Boolean {
    if (local is PwGroupV4 && cloud is PwGroupV4) {
      return differ.diff(local, cloud).hasDifferences()
    }
    if (local is PwGroupV3 && cloud is PwGroupV3) {
      return LegacyItemMerger.differs(local, cloud)
    }
    return cloud != local
  }

  private fun EntryDiff.hasDifferences(): Boolean {
    return strings.values.any { it.isDifferent() } ||
        binaries.values.any { it.isDifferent() } ||
        properties.values.any { it.isDifferent() } ||
        icon.isDifferent() ||
        customIcon.isDifferent() ||
        tags.isDifferent() ||
        expireDate.isDifferent() ||
        expires.isDifferent()
  }

  private fun GroupDiff.hasDifferences(): Boolean {
    return name.isDifferent() ||
        notes.isDifferent() ||
        properties.values.any { it.isDifferent() } ||
        icon.isDifferent() ||
        customIcon.isDifferent() ||
        tags.isDifferent() ||
        expireDate.isDifferent() ||
        expires.isDifferent()
  }

  private fun ThreeWay<*>.isDifferent(): Boolean = this !is ThreeWay.Same
}
