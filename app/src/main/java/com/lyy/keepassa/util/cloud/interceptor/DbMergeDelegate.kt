package com.lyy.keepassa.util.cloud.interceptor

import android.content.Intent
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwCustomData
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.service.feat.MutationOrigin
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.PwDataMap
import com.lyy.keepassa.util.cloud.merge.AutoMergerImpl
import com.lyy.keepassa.util.cloud.merge.DbDiffCollector
import com.lyy.keepassa.util.cloud.merge.Decision
import com.lyy.keepassa.util.cloud.merge.EntryDifferImpl
import com.lyy.keepassa.util.cloud.merge.FieldKey
import com.lyy.keepassa.util.cloud.merge.GroupChildOrderSynchronizer
import com.lyy.keepassa.util.cloud.merge.LegacyItemMerger
import com.lyy.keepassa.util.cloud.merge.MergeApplierImpl
import com.lyy.keepassa.util.cloud.merge.MergeConflictActivity
import com.lyy.keepassa.util.cloud.merge.MergeConflictItem
import com.lyy.keepassa.util.cloud.merge.MergeConflictResult
import com.lyy.keepassa.util.cloud.merge.MergeConflictSession
import com.lyy.keepassa.util.cloud.merge.MergeConflictSessionStore
import com.lyy.keepassa.util.cloud.merge.NewEntryApplicator
import com.lyy.keepassa.util.cloud.merge.syncCode
import com.lyy.keepassa.util.cloud.merge.pending.FilePendingMergeRepository
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeSnapshotStore
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeState
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeTaskDraft
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeTaskStore
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeNotificationManager
import com.lyy.keepassa.util.cloud.merge.pending.PendingMergeDatabaseIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import java.util.Date
import java.io.File
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:42 下午 2021/12/24
 **/
object DbMergeDelegate {
  /**
   * 对比云端和本地的数据库，并进行合并
   * @param isUpload 是否是上传
   * @return [DbSynUtil.STATE_SUCCEED] or [DbSynUtil.STATE_FAIL]
   */
  @ExperimentalCoroutinesApi
  suspend fun compareDb(
    record: DbHistoryRecord,
    cloudDb: PwDatabase,
    localDb: PwDatabase,
    isUpload: Boolean,
    onMergeFailed: ((Int) -> Unit)? = null,
    mergeInteractionMode: MergeInteractionMode = MergeInteractionMode.FOREGROUND,
    cloudSnapshot: File? = null,
    cloudModifiedTime: Long? = null
  ): Int {
    val compareLists = DbDiffCollector().collect(cloudDb, localDb)
    val modifyList = compareLists.modifyList // 有改动的条目，first 为云端的条目，second 为本地的条目
    val delList = compareLists.delList // 本地有而云端没有的条目
    val newList = compareLists.newList // 本地没有的条目
    val moveList = compareLists.moveList // 被移动的条目，first 为云端的条目，second 为本地的条目
    val hasOrderChanges = GroupChildOrderSynchronizer().hasDifferences(cloudDb, localDb)

    Timber.i(
      "比对数据完成，newListSize = ${newList.size}，moveListSize = ${moveList.size}，delListSize = ${delList.size}，modifyListSize = ${modifyList.size}，hasOrderChanges = $hasOrderChanges"
    )

    if (newList.size == 0 && moveList.size == 0 && delList.size == 0 && modifyList.size == 0 && !hasOrderChanges) {
      Timber.i("对比结果：无新增，无删除，无移动，无修改，忽略该次上传，并更新缓存的云端文件修改时间")
      DbSynUtil.updateServiceModifyTime(record)
      return DbSynUtil.STATE_SUCCEED
    }

    val mergeItems = buildMergeItems(modifyList)
    val autoResolvedItems = mergeItems.filter { it.autoMerge.conflicts.isEmpty() }
    val conflictItems = mergeItems.filter { it.autoMerge.conflicts.isNotEmpty() }
    if (conflictItems.isEmpty() && delList.isEmpty()) {
      applyAutomaticChanges(newList, moveList, cloudDb, localDb)
      applyMergeDecisions(autoResolvedItems, emptyMap(), localDb)
      val saveCode = KpaUtil.kdbHandlerService.saveDbAwait()
      if (saveCode != DbSynUtil.STATE_SUCCEED) {
        return saveCode
      }
      return DbSynUtil.STATE_SUCCEED
    }

    val snapshot = requireNotNull(cloudSnapshot) {
      "Merge conflict requires a downloaded cloud snapshot"
    }
    val pendingDirectory = File(BaseApp.APP.filesDir, PendingMergeSnapshotStore.ROOT_DIRECTORY)
    val pendingRepository = FilePendingMergeRepository(pendingDirectory)
    val pendingSnapshots = PendingMergeSnapshotStore(BaseApp.APP.filesDir)
    val databaseIdentity = PendingMergeDatabaseIdentity.resolve(
      BaseApp.dbRecord?.localDbUri,
      record.localDbUri
    )
    val pendingTask = PendingMergeTaskStore(
      repository = pendingRepository,
      snapshots = pendingSnapshots
    ).create(
        draft = PendingMergeTaskDraft(
          id = UUID.randomUUID().toString(),
          databaseIdentity = databaseIdentity,
          localDatabaseUri = databaseIdentity,
          cloudStorageType = record.type,
          cloudDatabasePath = record.cloudDiskPath.orEmpty(),
          cloudModifiedTime = cloudModifiedTime,
          cloudContentHash = null,
          createdAt = System.currentTimeMillis(),
          state = PendingMergeState.WAITING_FOR_UNLOCK
        ),
        cloudDatabase = snapshot
      )

    if (mergeInteractionMode == MergeInteractionMode.BACKGROUND) {
      PendingMergeNotificationManager.notify(pendingTask.id)
      return DbSynUtil.STATE_MERGE_PENDING
    }

    Timber.i("有字段级冲突或本地独有数据，启动合并冲突界面")
    val result = showMergeConflictActivity(conflictItems, delList, onMergeFailed)
    if (result !is MergeConflictResult.Resolved) {
      pendingRepository.remove(pendingTask.id)
      pendingSnapshots.remove(pendingTask.id)
      return result.syncCode
    }

    applyAutomaticChanges(newList, moveList, cloudDb, localDb)
    applyMergeDecisions(autoResolvedItems, emptyMap(), localDb)
    applyMergeDecisions(conflictItems, result.decisions, localDb)
    val deleteItems = result.deleteLocalOnlyIndexes.mapNotNull(delList::getOrNull)
    val deleteEntryIds = deleteItems.filterIsInstance<PwEntry>().map { it.uuid }.toSet()
    val deleteGroupIds = deleteItems.filterIsInstance<PwGroup>().map { it.id }.toSet()
    result.deleteLocalOnlyIndexes.forEach { index ->
      delList.getOrNull(index)?.let {
        if (it is PwGroup) {
          preserveUnselectedChildren(it, deleteEntryIds, deleteGroupIds, localDb)
        }
        deleteLocalOnly(it, localDb)
      }
    }
    val saveCode = KpaUtil.kdbHandlerService.saveDbAwait()
    if (saveCode != DbSynUtil.STATE_SUCCEED) {
      return saveCode
    }
    pendingRepository.remove(pendingTask.id)
    pendingSnapshots.remove(pendingTask.id)
    return DbSynUtil.STATE_SUCCEED
  }

  private suspend fun applyAutomaticChanges(
    newList: ArrayList<PwDataInf>,
    moveList: ArrayList<PwDataMap>,
    cloudDb: PwDatabase,
    localDb: PwDatabase
  ) {
    if (newList.isNotEmpty()) {
      Timber.i("本地需要新增条目")
      localAddNewEntry(newList, localDb)
    }
    if (moveList.isNotEmpty()) {
      Timber.i("本地需要移动条目")
      moveLocalEntry(moveList, localDb)
    }
    GroupChildOrderSynchronizer().apply(cloudDb, localDb)
  }

  private fun buildMergeItems(
    modifyList: ArrayList<Pair<PwDataInf, PwDataInf>>
  ): ArrayList<MergeConflictItem> {
    val differ = EntryDifferImpl()
    val autoMerger = AutoMergerImpl()
    val items = ArrayList<MergeConflictItem>()
    for (p in modifyList) {
      val cloud = p.first
      val local = p.second
      when {
        cloud is PwEntryV4 && local is PwEntryV4 -> {
          val autoMerge = autoMerger.merge(differ.diff(local, cloud))
          items.add(MergeConflictItem(cloud, local, autoMerge))
        }
        cloud is PwEntry && local is PwEntry -> {
          items.add(MergeConflictItem(cloud, local, LegacyItemMerger.conflict(local, cloud)))
        }
        cloud is PwGroupV4 && local is PwGroupV4 -> {
          val autoMerge = autoMerger.merge(differ.diff(local, cloud))
          items.add(MergeConflictItem(cloud, local, autoMerge))
        }
        cloud is PwGroup && local is PwGroup -> {
          items.add(MergeConflictItem(cloud, local, LegacyItemMerger.conflict(local, cloud)))
        }
      }
    }
    return items
  }

  private suspend fun showMergeConflictActivity(
    items: List<MergeConflictItem>,
    localOnlyItems: List<PwDataInf>,
    onMergeFailed: ((Int) -> Unit)?
  ): MergeConflictResult {
    val session = MergeConflictSession(
      items = items,
      localOnlyItems = localOnlyItems,
      onMergeFailed = onMergeFailed
    )
    MergeConflictSessionStore.put(session)
    BaseApp.APP.startActivity(
      Intent(BaseApp.APP, MergeConflictActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(MergeConflictActivity.KEY_SESSION_ID, session.id)
    )
    return try {
      session.channel.receive()
    } finally {
      session.channel.cancel()
    }
  }

  private fun applyMergeDecisions(
    items: List<MergeConflictItem>,
    decisionsByIndex: Map<Int, Map<FieldKey, Decision>>,
    localDb: PwDatabase
  ) {
    val applier = MergeApplierImpl()
    items.forEachIndexed { index, item ->
      val decisions = decisionsByIndex[index].orEmpty()
      when {
        item.cloud is PwEntryV4 && item.local is PwEntryV4 -> {
          assignMergedEntry(
            local = item.local,
            merged = applier.apply(
              local = item.local,
              autoMerge = item.autoMerge,
              decisions = decisions,
              database = localDb as? PwDatabaseV4
            )
          )
        }
        item.cloud is PwEntry && item.local is PwEntry -> {
          val decision = decisions[FieldKey.LegacyItem]
            ?: throw IllegalArgumentException("Missing legacy entry merge decision")
          assignMergedEntry(
            local = item.local,
            merged = LegacyItemMerger.apply(item.local, item.cloud, decision) as PwEntry
          )
        }
        item.cloud is PwGroupV4 && item.local is PwGroupV4 -> {
          assignMergedGroup(
            local = item.local,
            merged = applier.apply(
              local = item.local,
              autoMerge = item.autoMerge,
              decisions = decisions,
              database = localDb as? PwDatabaseV4
            )
          )
        }
        item.cloud is PwGroup && item.local is PwGroup -> {
          val decision = decisions[FieldKey.LegacyItem]
            ?: throw IllegalArgumentException("Missing legacy group merge decision")
          assignMergedGroup(
            local = item.local,
            merged = LegacyItemMerger.apply(item.local, item.cloud, decision) as PwGroup
          )
        }
      }
    }
  }

  private fun assignMergedEntry(
    local: PwEntry,
    merged: PwEntry
  ) {
    local.assign(merged)
    if (local is PwEntryV4 && merged is PwEntryV4) {
      local.tags = merged.tags
      local.customData = merged.customData.copyForMerge()
      local.prevParentGroup = merged.prevParentGroup
      local.qualityCheck = merged.qualityCheck
    }
  }

  private fun assignMergedGroup(
    local: PwGroup,
    merged: PwGroup
  ) {
    local.assign(merged)
    if (local is PwGroupV4 && merged is PwGroupV4) {
      local.tags = merged.tags
      local.customData = merged.customData.copyForMerge()
      local.prevParentGroup = merged.prevParentGroup
      local.lastTopVisibleEntry = merged.lastTopVisibleEntry
    }
  }

  private fun PwCustomData.copyForMerge(): PwCustomData {
    return PwCustomData().also { copy ->
      copy.putAll(this)
      copy.lastMod = lastMod.mapValues { (_, value) -> Date(value.time) }.toMutableMap()
    }
  }

  private fun deleteLocalOnly(
    item: PwDataInf,
    localDb: PwDatabase
  ) {
    when (item) {
      is PwEntry -> {
        if (localDb.canRecycle(item)) {
          localDb.recycle(item)
        } else {
          localDb.deleteEntry(item)
        }
      }
      is PwGroupV4 -> {
        if (localDb is PwDatabaseV4 && localDb.canRecycle(item)) {
          localDb.recycle(item)
        } else {
          removeGroupTree(item, localDb)
        }
      }
      is PwGroup -> {
        removeGroupTree(item, localDb)
      }
    }
  }

  private fun preserveUnselectedChildren(
    group: PwGroup,
    deleteEntryIds: Set<java.util.UUID>,
    deleteGroupIds: Set<com.keepassdroid.database.PwGroupId>,
    localDb: PwDatabase
  ) {
    val destination = group.parent ?: localDb.rootGroup
    group.childEntries.toList().forEach { entry ->
      if (entry.uuid !in deleteEntryIds) {
        localDb.moveEntry(entry, destination)
      }
    }
    group.childGroups.toList().forEach { child ->
      if (child.id in deleteGroupIds) {
        preserveUnselectedChildren(child, deleteEntryIds, deleteGroupIds, localDb)
      } else {
        localDb.moveGroup(child, destination)
      }
    }
  }

  private fun removeGroupTree(
    group: PwGroup,
    localDb: PwDatabase
  ) {
    for (entry in group.childEntries.toList()) {
      localDb.entries.remove(entry.uuid)
    }
    for (child in group.childGroups.toList()) {
      removeGroupTree(child, localDb)
    }
    group.parent?.let {
      localDb.removeGroupFrom(group, it)
    }
    localDb.groups.remove(group.id)
  }

  /**
   * 移动数据
   * @param needMoveList 本地需要移动的数据，first 为云端的条目，second 为本地的条目
   */
  private fun moveLocalEntry(
    needMoveList: ArrayList<PwDataMap>,
    localDb: PwDatabase
  ) {
    for (p in needMoveList) {
      if (p.cloudPwData is PwEntry) {
        localDb.moveEntry(p.localPwData as PwEntry, getParentByCloudPwData(p.cloudPwData, localDb))
        continue
      }
      // 处理群组的移动
      localDb.moveGroup(p.localPwData as PwGroup, getParentByCloudPwData(p.cloudPwData, localDb))
    }
  }

  /**
   * 本地新增云端有而本地没的条目
   * @param newList 云端服务器新增加的条目列表
   */
  private suspend fun localAddNewEntry(
    newList: ArrayList<PwDataInf>,
    localDb: PwDatabase
  ) {
    val plan = NewEntryApplicator().plan(newList, localDb)
    for (orphan in plan.orphanGroups) {
      Timber.w("云端新增群组 [${orphan.name}] 的父群组不存在,将该群组回退至根群组并保留其子级结构")
    }
    for (orphan in plan.orphanEntries) {
      Timber.w("云端新增条目 [${orphan.title}] 的父群组不存在,将该条目回退至根群组")
    }
    for (newGroup in plan.groupAdds) {
      KpaUtil.kdbHandlerService.addGroup(newGroup, localDb, MutationOrigin.CLOUD)
    }
    for (newEntry in plan.entryAdds) {
      KpaUtil.kdbHandlerService.createEntry(
        newEntry,
        database = localDb,
        origin = MutationOrigin.CLOUD
      )
    }
  }

  /**
   * 通过云端条目获取本地条目
   */
  private fun getParentByCloudPwData(
    cloudPwDataInf: PwDataInf,
    localDb: PwDatabase
  ): PwGroup {
    var parent = localDb.rootGroup

    if (cloudPwDataInf.parent != null) {
      val temp = localDb.groups[cloudPwDataInf.parent.id]
      if (temp != null) {
        parent = temp
      }
    }
    return parent
  }
}
