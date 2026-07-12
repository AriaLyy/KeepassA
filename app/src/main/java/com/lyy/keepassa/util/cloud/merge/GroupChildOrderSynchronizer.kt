package com.lyy.keepassa.util.cloud.merge

import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import java.util.UUID

class GroupChildOrderSynchronizer {

  fun hasDifferences(cloudDb: PwDatabase, localDb: PwDatabase): Boolean {
    return cloudGroups(cloudDb).any { cloudGroup ->
      val localGroup = findLocalGroup(cloudGroup, cloudDb, localDb) ?: return@any false
      commonEntryIds(cloudGroup, localGroup).let { (cloudIds, localIds) ->
        cloudIds != localIds
      } || commonGroupIds(cloudGroup, localGroup).let { (cloudIds, localIds) ->
        cloudIds != localIds
      }
    }
  }

  fun apply(cloudDb: PwDatabase, localDb: PwDatabase) {
    cloudGroups(cloudDb).forEach { cloudGroup ->
      val localGroup = findLocalGroup(cloudGroup, cloudDb, localDb) ?: return@forEach
      localGroup.childEntries = reorder(
        cloud = cloudGroup.childEntries,
        local = localGroup.childEntries.filter { it.parent === localGroup },
        id = { it.uuid }
      )
      localGroup.childGroups = reorder(
        cloud = cloudGroup.childGroups,
        local = localGroup.childGroups.filter { it.parent === localGroup },
        id = { it.id }
      )
    }
  }

  private fun cloudGroups(database: PwDatabase): List<PwGroup> {
    return buildList {
      add(database.rootGroup)
      database.groups.values.forEach { group ->
        if (group.id != database.rootGroup.id) {
          add(group)
        }
      }
    }
  }

  private fun findLocalGroup(
    cloudGroup: PwGroup,
    cloudDb: PwDatabase,
    localDb: PwDatabase
  ): PwGroup? {
    return if (cloudGroup.id == cloudDb.rootGroup.id) {
      localDb.rootGroup.takeIf { it.id == cloudGroup.id }
    } else {
      localDb.groups[cloudGroup.id]
    }
  }

  private fun commonEntryIds(
    cloudGroup: PwGroup,
    localGroup: PwGroup
  ): Pair<List<UUID>, List<UUID>> {
    val cloudIds = cloudGroup.childEntries.map { it.uuid }
    val localIds = localGroup.childEntries
      .filter { it.parent === localGroup }
      .map { it.uuid }
    return Pair(
      cloudIds.filter { it in localIds },
      localIds.filter { it in cloudIds }
    )
  }

  private fun commonGroupIds(
    cloudGroup: PwGroup,
    localGroup: PwGroup
  ): Pair<List<PwGroupId>, List<PwGroupId>> {
    val cloudIds = cloudGroup.childGroups.map { it.id }
    val localIds = localGroup.childGroups
      .filter { it.parent === localGroup }
      .map { it.id }
    return Pair(
      cloudIds.filter { it in localIds },
      localIds.filter { it in cloudIds }
    )
  }

  private fun <T, K> reorder(
    cloud: List<T>,
    local: List<T>,
    id: (T) -> K
  ): ArrayList<T> {
    val localById = local.associateBy(id)
    val consumed = linkedSetOf<K>()
    return ArrayList<T>().apply {
      cloud.forEach { cloudItem ->
        val itemId = id(cloudItem)
        localById[itemId]?.let { localItem ->
          add(localItem)
          consumed += itemId
        }
      }
      local.forEach { localItem ->
        if (id(localItem) !in consumed) {
          add(localItem)
        }
      }
    }
  }
}
