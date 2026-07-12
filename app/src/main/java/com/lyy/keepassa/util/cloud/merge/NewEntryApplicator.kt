package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import java.util.Date

class NewEntryApplicator {

  data class Result(
    val groupAdds: List<PwGroupV4>,
    val entryAdds: List<PwEntryV4>,
    val orphanGroups: List<PwGroupV4>,
    val orphanEntries: List<PwEntryV4>
  )

  fun plan(
    newList: List<PwDataInf>,
    localDb: PwDatabase
  ): Result {
    val cloudGroups = newList.filterIsInstance<PwGroupV4>()
    val cloudEntries = newList.filterIsInstance<PwEntryV4>()

    val groupAdds = mutableListOf<PwGroupV4>()
    val orphanGroups = mutableListOf<PwGroupV4>()
    val clonedById = mutableMapOf<PwGroupId, PwGroupV4>()
    val pendingGroups = cloudGroups.toMutableList()
    while (pendingGroups.isNotEmpty()) {
      var resolvedAny = false
      val iterator = pendingGroups.iterator()
      while (iterator.hasNext()) {
        val cloudGroup = iterator.next()
        val resolvedParent = resolveParent(cloudGroup, localDb, clonedById) ?: continue
        val clone = cloneGroup(cloudGroup, resolvedParent)
        clonedById[clone.id] = clone
        groupAdds.add(clone)
        iterator.remove()
        resolvedAny = true
      }
      if (resolvedAny) {
        continue
      }

      val pendingIds = pendingGroups.map { it.id }.toSet()
      val fallbackIndex = pendingGroups.indexOfFirst { group ->
        group.parent?.id !in pendingIds
      }.takeIf { it >= 0 } ?: 0
      val orphan = pendingGroups.removeAt(fallbackIndex)
      val fallbackParent = localDb.rootGroup as? PwGroupV4 ?: continue
      val clone = cloneGroup(orphan, fallbackParent)
      orphanGroups.add(orphan)
      clonedById[clone.id] = clone
      groupAdds.add(clone)
    }

    val entryAdds = mutableListOf<PwEntryV4>()
    val orphanEntries = mutableListOf<PwEntryV4>()
    for (cloudEntry in cloudEntries) {
      val resolvedParent = resolveParent(cloudEntry, localDb, clonedById)
        ?: (localDb.rootGroup as? PwGroupV4)?.also {
          orphanEntries.add(cloudEntry)
        } ?: continue
      val clone = cloneEntry(cloudEntry, resolvedParent)
      entryAdds.add(clone)
    }

    return Result(
      groupAdds = groupAdds,
      entryAdds = entryAdds,
      orphanGroups = orphanGroups,
      orphanEntries = orphanEntries
    )
  }

  private fun cloneGroup(
    cloudGroup: PwGroupV4,
    parent: PwGroupV4
  ): PwGroupV4 {
    return (cloudGroup.clone() as PwGroupV4).also { clone ->
      clone.childGroups = ArrayList()
      clone.childEntries = ArrayList()
      clone.customData = cloudGroup.customData.copyForAdd()
      clone.parent = parent
    }
  }

  private fun cloneEntry(
    cloudEntry: PwEntryV4,
    parent: PwGroupV4
  ): PwEntryV4 {
    return cloudEntry.cloneDeep().also { clone ->
      clone.customData = cloudEntry.customData.copyForAdd()
      clone.parent = parent
    }
  }

  private fun PwCustomData.copyForAdd(): PwCustomData {
    return PwCustomData().also { copy ->
      copy.putAll(this)
      copy.lastMod = lastMod.mapValues { (_, value) -> Date(value.time) }.toMutableMap()
    }
  }

  private fun resolveParent(
    data: PwDataInf,
    localDb: PwDatabase,
    clonedById: Map<PwGroupId, PwGroupV4>
  ): PwGroupV4? {
    val cloudParent = data.parent ?: return localDb.rootGroup as? PwGroupV4
    clonedById[cloudParent.id]?.let { return it }
    return localDb.groups[cloudParent.id] as? PwGroupV4
  }
}
