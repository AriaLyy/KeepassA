/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
import android.net.Uri
import android.content.ContextWrapper
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.keepassdroid.Database
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.event.CollectionEvent
import com.lyy.keepassa.event.CollectionEventType
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_ADD
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_REMOVE
import com.lyy.keepassa.event.EntryState.CREATE
import com.lyy.keepassa.event.EntryState.DELETE
import com.lyy.keepassa.event.EntryState.MODIFY
import com.lyy.keepassa.event.EntryState.MOVE
import com.lyy.keepassa.event.EntryState.SAVE
import com.lyy.keepassa.event.EntryStateChangeEvent
import com.lyy.keepassa.event.GroupStateChangeEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil.isNull
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.interceptor.MergeInteractionMode
import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse
import com.lyy.keepassa.util.cloud.merge.MergeConflictSessionStore
import com.lyy.keepassa.util.setCollection
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal data class SaveTransactionResult(
  val code: Int,
  val snapshot: SavedDatabaseSnapshot? = null
)

enum class MutationOrigin { LOCAL, CLOUD }

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:03 下午 2022/3/24
 **/
@Route(path = "/service/kdbHandler")
class KdbHandlerService : IProvider {
  companion object {
    const val MIN_TIME = 200L
  }

  private var scope = MainScope()
  private var collectionNum = AtomicInteger(0)
  private val syncRevision by lazy {
    SyncRevisionCoordinator(SharedPreferencesSyncRevisionRepository(BaseApp.APP))
  }

  fun markLocalChange(): Long {
    val databaseId = BaseApp.dbRecord?.localDbUri ?: return 0
    return syncRevision.localChanged(databaseId)
  }

  /**
   * collection state flow
   */
  val collectionStateFlow = MutableStateFlow(CollectionEvent())

  val entryStateChangeFlow = MutableSharedFlow<EntryStateChangeEvent>()
  val groupStateChangeFlow = MutableStateFlow(GroupStateChangeEvent())
  private val collectionEntries = hashSetOf<PwEntryV4>()
  private val mutex = Mutex()

  private val loadingDialog: LoadingDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getLoadingDialog()
  }

  private val kdbHelper by lazy {
    KDBHandlerHelper.getInstance(BaseApp.APP)
  }

  fun clearDb() {
    BaseApp.KDB?.clear(BaseApp.APP)
    BaseApp.KDB = null
    collectionEntries.clear()
    collectionNum.set(0)
  }

  fun getCollectionEntries() = collectionEntries
  fun getCollectionNum() = collectionNum.get()

  internal fun updateCollectionEntries(collectionEntries: Set<PwEntryV4>) {
    this.collectionEntries.clear()
    this.collectionEntries.addAll(collectionEntries)
  }

  internal fun updateCollectionNum(newCollectionNum: Int) {
    collectionNum.set(newCollectionNum)
    scope.launch {
      collectionStateFlow.emit(
        CollectionEvent(
          state = CollectionEventType.COLLECTION_STATE_TOTAL,
          collectionNum = collectionNum.get()
        )
      )
    }
  }

  /**
   * @param collection true: add collection, false: cancel collection
   */
  fun collection(pwEntryV4: PwEntryV4, collection: Boolean) {
    pwEntryV4.setCollection(collection)
    markLocalChange()
    if (collection) {
      collectionEntries.add(pwEntryV4)
    } else {
      collectionEntries.remove(pwEntryV4)
    }
    scope.launch {
      if (collection) {
        collectionStateFlow.emit(
          CollectionEvent(
            COLLECTION_STATE_ADD,
            collectionNum.incrementAndGet(),
            pwEntryV4
          )

        )
        return@launch
      }
      collectionStateFlow.emit(
        CollectionEvent(
          COLLECTION_STATE_REMOVE,
          collectionNum.decrementAndGet(),
          pwEntryV4
        )
      )
    }
  }

  private fun showLoading() {
    Timber.d("showLoading, hashCode = ${loadingDialog.hashCode()}")
    scope.launch {
      if (loadingDialog.isVisible) {
        return@launch
      }
      loadingDialog.show()
    }
  }

  private fun dismissLoading(delay: Long = 0) {
    Timber.d("dismissLoading, delay = ${delay}")
    scope.launch {
      loadingDialog.dismiss(delay)
    }
  }

  /**
   * delete group
   */
  fun deleteGroup(pwGroup: PwGroupV4, callback: () -> Unit) {
    scope.launch {
      val oldParent = pwGroup.parent
      withContext(Dispatchers.IO) {
        if (BaseApp.KDB!!.pm.canRecycle(pwGroup)) {
          (BaseApp.KDB!!.pm as PwDatabaseV4).recycle(pwGroup)
        } else {
          kdbHelper.deleteGroup(BaseApp.KDB, pwGroup, true)
        }
      }
      markLocalChange()
      callback.invoke()
      groupStateChangeFlow.emit(GroupStateChangeEvent(DELETE, pwGroup, oldParent))
    }
  }

  /**
   * only send status
   */
  fun updateEntryStatus(v4Entry: PwEntryV4) {
    scope.launch {
      v4Entry.touch(true, true)
      markLocalChange()
      entryStateChangeFlow.emit(
        EntryStateChangeEvent(
          MODIFY,
          v4Entry,
          v4Entry.parent
        )
      )
    }
  }

  /**
   * move entry from other group
   * @param targetParent target parent
   */
  fun moveEntry(v4Entry: PwEntryV4, targetParent: PwGroupV4) {
    scope.launch {
      val originParent = v4Entry.parent
      withContext(Dispatchers.IO) {
        v4Entry.parent.childEntries.remove(v4Entry)

        if (v4Entry.parent == BaseApp.KDB.pm.recycleBin) {
          (BaseApp.KDB.pm as PwDatabaseV4).undoRecycle(v4Entry, targetParent)
        } else {
          (BaseApp.KDB.pm as PwDatabaseV4).moveEntry(v4Entry, targetParent)
        }
      }
      markLocalChange()
      entryStateChangeFlow.emit(
        EntryStateChangeEvent(
          MOVE,
          v4Entry,
          originParent
        )
      )
    }
  }

  /**
   * delete entry
   */
  fun deleteEntry(v4Entry: PwEntryV4, callback: () -> Unit) {
    scope.launch {
      val parent = v4Entry.parent
      withContext(Dispatchers.IO) {
        kdbHelper.deleteEntry(BaseApp.KDB, v4Entry, true)
      }
      markLocalChange()
      callback.invoke()
      entryStateChangeFlow.emit(EntryStateChangeEvent(DELETE, v4Entry, parent))
    }
  }

  /**
   * update group info and send new state
   */
  fun modifyGroup(
    groupName: String,
    icon: PwIconStandard,
    customIcon: PwIconCustom?,
    self: PwGroupV4,
    callback: () -> Unit
  ) {
    scope.launch {
      withContext(Dispatchers.IO) {
        self.customIcon = customIcon
        self.icon = icon
        self.name = groupName
        if (kdbHelper.save(BaseApp.KDB)) {
          BaseApp.KDB.dirty.add(self.parent)
        }
      }

      markLocalChange()
      callback.invoke()
      groupStateChangeFlow.emit(GroupStateChangeEvent(MODIFY, self))
    }
  }

  /**
   * create new Group
   *
   * @param icon default icon
   * @param customIcon custom icon
   */
  fun createGroup(
    groupName: String,
    icon: PwIconStandard,
    customIcon: PwIconCustom?,
    parent: PwGroupV4,
    callback: (PwGroupV4) -> Unit
  ) {
    scope.launch {
      val tempGroup = withContext(Dispatchers.IO) {
        val pm: PwDatabase = BaseApp.KDB.pm

        val group = pm.createGroup() as PwGroupV4
        group.initNewGroup(groupName, pm.newGroupId())
        group.icon = icon
        customIcon?.let { group.customIcon = it }
        pm.addGroupTo(group, parent)

        return@withContext group
      }
      markLocalChange()
      callback.invoke(tempGroup)
      groupStateChangeFlow.emit(GroupStateChangeEvent(CREATE, tempGroup, null))
    }
  }

  /**
   * add new group
   */
  fun createGroup(group: PwGroup) {
    kdbHelper
      .createGroup(BaseApp.KDB, group.name, group.icon, group.parent)
    markLocalChange()
  }

  fun addGroup(group: PwGroupV4) {
    BaseApp.KDB?.pm?.let { addGroup(group, it) }
  }

  private val mergeCompletionLoading = AtomicBoolean(false)

  fun showMergeCompletionLoading() {
    mergeCompletionLoading.set(true)
    showLoading()
  }

  fun dismissMergeCompletionLoading() {
    if (mergeCompletionLoading.compareAndSet(true, false)) {
      dismissLoading()
    }
  }

  suspend fun runForegroundUploadWithLoading(
    upload: suspend () -> DbSyncResponse
  ): DbSyncResponse {
    withContext(Dispatchers.Main) {
      if (!loadingDialog.isVisible) loadingDialog.show()
    }
    var responseCode = DbSynUtil.STATE_FAIL
    return try {
      upload().also { responseCode = it.code }
    } finally {
      MergeConflictSessionStore.completeResolvedSessions(responseCode)
      withContext(NonCancellable + Dispatchers.Main) {
        loadingDialog.dismiss(0)
      }
    }
  }

  fun addGroup(
    group: PwGroupV4,
    database: PwDatabase,
    origin: MutationOrigin = MutationOrigin.LOCAL
  ) {
    database.addGroupTo(group, group.parent)
    (database as? PwDatabaseV4)?.let { target -> registerGroupReference(group, target) }
    if (origin == MutationOrigin.LOCAL) markLocalChange()
  }

  /**
   * add new entry
   */
  fun createEntry(entry: PwEntryV4, parent: PwGroup? = null) {
    createEntry(entry, parent, BaseApp.KDB!!.pm)
  }

  fun createEntry(
    entry: PwEntryV4,
    parent: PwGroup? = null,
    database: PwDatabase,
    origin: MutationOrigin = MutationOrigin.LOCAL
  ) {
    database.addEntryTo(entry, parent ?: entry.parent)
    (database as? PwDatabaseV4)?.let { target -> registerEntryReferences(entry, target) }
    if (origin == MutationOrigin.LOCAL) markLocalChange()
    scope.launch {
      entryStateChangeFlow.emit(EntryStateChangeEvent(CREATE, entry))
    }
  }

  /**
   * only add entry
   */
  fun addEntryTo(entry: PwEntryV4, parent: PwGroup) {
    BaseApp.KDB!!.pm.addEntryTo(entry, parent)
    markLocalChange()
  }

  suspend fun saveOnly(needShowLoading: Boolean = false, callback: (Int) -> Unit) {
    mutex.withLock {
      withContext(Dispatchers.IO) {
        if (needShowLoading) {
          showLoading()
        }

        val b = kdbHelper.save(BaseApp.KDB)
        if (needShowLoading) {
          dismissLoading()
        }
        withContext(Dispatchers.Main) {
          callback.invoke(if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL)
          entryStateChangeFlow.emit(EntryStateChangeEvent(SAVE))
        }
      }
    }
  }

  /**
   * save db by background
   * @param uploadDb true: upload db to cloud
   * @param callback run in main thread
   */
  fun saveDbByBackground(uploadDb: Boolean = false, callback: (Int) -> Unit = {}) {
    Timber.d("start save db by background")
    if (BaseApp.KDB.isNull()) {
      Timber.d("db is null")
      callback.invoke(DbSynUtil.STATE_SAVE_DB_FAIL)
      return
    }
    if (BaseApp.isLocked) {
      Timber.d("db is locked")
      callback.invoke(DbSynUtil.STATE_CANCEL)
      return
    }
    val databaseId = BaseApp.dbRecord?.localDbUri.orEmpty()
    val needsUpload = databaseId.isNotEmpty() && syncRevision.needsUpload(databaseId)
    if (uploadDb && !BackgroundSavePolicy.shouldSave(needsUpload || BaseApp.KDB!!.dirty.isNotEmpty())) {
      Timber.d("database has no unsaved changes, skip background save and upload")
      callback.invoke(DbSynUtil.STATE_SUCCEED)
      return
    }
    scope.launch(Dispatchers.IO) {
      val code = try {
        if (uploadDb) {
          delay(1000)
          val response = saveAndUploadSnapshot(
            isCreate = false,
            mergeInteractionMode = MergeInteractionMode.BACKGROUND
          )
          Timber.i(response.msg)
          response.code
        } else {
          saveDbAwait()
        }
      } catch (error: CancellationException) {
        withContext(NonCancellable + Dispatchers.Main) {
          callback.invoke(DbSynUtil.STATE_CANCEL)
        }
        throw error
      } catch (error: Exception) {
        Timber.e(error, "后台保存上传失败")
        DbSynUtil.STATE_FAIL
      }
      withContext(NonCancellable + Dispatchers.Main) {
        callback.invoke(code)
      }
      if (code != DbSynUtil.STATE_CANCEL) {
        entryStateChangeFlow.emit(EntryStateChangeEvent(SAVE))
      }
    }
  }

  suspend fun saveDbAwait(): Int {
    return saveDbTransactionAwait(createSnapshot = false).code
  }

  private suspend fun saveDbTransactionAwait(createSnapshot: Boolean): SaveTransactionResult {
    return mutex.withLock {
      val kdb = BaseApp.KDB
        ?: return@withLock SaveTransactionResult(DbSynUtil.STATE_SAVE_DB_FAIL)
      val databaseFile = BaseApp.dbRecord?.getDbUri()?.takeIf { it.scheme == "file" }?.path?.let(::File)
      val saved = withContext(Dispatchers.IO) {
        if (databaseFile == null) {
          kdbHelper.save(kdb)
        } else {
          LocalDatabaseSaveGuard().save(
            database = databaseFile,
            write = { kdbHelper.save(kdb) },
            validate = { validateSavedDatabase(databaseFile) }
          )
        }
      }
      Timber.d("保存后的数据库hash：${kdb.hashCode()}，num = ${kdb.pm?.entries?.size ?: 0}")
      if (!saved) return@withLock SaveTransactionResult(DbSynUtil.STATE_SAVE_DB_FAIL)
      if (!createSnapshot || databaseFile == null) {
        return@withLock SaveTransactionResult(DbSynUtil.STATE_SUCCEED)
      }
      val databaseId = BaseApp.dbRecord?.localDbUri.orEmpty()
      val revision = syncRevision.captureUploadRevision(databaseId)
      val store = SavedDatabaseSnapshotStore(File(BaseApp.APP.cacheDir, "saved_database_snapshots"))
      SaveTransactionResult(DbSynUtil.STATE_SUCCEED, store.create(databaseFile, revision))
    }
  }

  private suspend fun saveAndUploadSnapshot(
    isCreate: Boolean,
    mergeInteractionMode: MergeInteractionMode,
    onMergeFailed: ((Int) -> Unit)? = null
  ): com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse {
    val transaction = saveDbTransactionAwait(createSnapshot = true)
    val snapshot = transaction.snapshot
      ?: return com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse(
        transaction.code,
        "save database failed"
      )
    val store = SavedDatabaseSnapshotStore(File(BaseApp.APP.cacheDir, "saved_database_snapshots"))
    val response = SavedSnapshotUploadSequence.run(
      snapshot = snapshot,
      upload = { frozen ->
        val record = BaseApp.dbRecord
          ?: return@run com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse(
            DbSynUtil.STATE_SAVE_DB_FAIL,
            "database record missing"
          )
        DbSynUtil.uploadSyn(
          record.copy(localDbUri = Uri.fromFile(frozen.file).toString()),
          isCreate,
          onMergeFailed,
          mergeInteractionMode
        )
      },
      cleanup = store::delete
    )
    if (response.code == DbSynUtil.STATE_SUCCEED) {
      val databaseId = BaseApp.dbRecord?.localDbUri.orEmpty()
      if (databaseId.isNotEmpty()) {
        syncRevision.uploadSucceeded(databaseId, snapshot.revision)
      }
    }
    return response
  }

  private fun validateSavedDatabase(databaseFile: File): Boolean {
    return try {
      TemporaryValidationWorkspace(File(BaseApp.APP.cacheDir, "save_validation")).use { directory ->
        val validationContext = object : ContextWrapper(BaseApp.APP) {
          override fun getFilesDir(): File = directory
        }
        val verified = Database().apply {
          LoadDataStrict(
            validationContext,
            Uri.fromFile(databaseFile),
            QuickUnLockUtil.decryption(BaseApp.dbPass),
            BaseApp.dbKeyPath.takeIf { it.isNotEmpty() }
              ?.let(QuickUnLockUtil::decryption)
              ?.let(Uri::parse)
          )
        }
        val valid = verified.pm != null
        verified.clear(validationContext)
        if (!valid) Timber.e("保存后的数据库无法重新打开，已拒绝该保存结果")
        valid
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Timber.e(error, "保存后的数据库验证失败，已恢复保存前文件")
      false
    }
  }

  private fun registerEntryReferences(entry: PwEntryV4, database: PwDatabaseV4) {
    entry.binaries.values.forEach(database.binPool::poolAdd)
    entry.customIcon?.takeIf(CustomIconSavePolicy::isSerializable)?.let { icon ->
      if (icon !in database.customIcons) database.customIcons.add(icon)
    }
    entry.history.filterIsInstance<PwEntryV4>().forEach { registerEntryReferences(it, database) }
  }

  private fun registerGroupReference(group: PwGroupV4, database: PwDatabaseV4) {
    group.customIcon?.takeIf(CustomIconSavePolicy::isSerializable)?.let { icon ->
      if (icon !in database.customIcons) database.customIcons.add(icon)
    }
  }

  /**
   * save db
   * @param uploadDb true: upload db to cloud
   * @param isCreate true: is create new db, save that db and upload it.
   * @param needShowLoading do you need to display the load dialog box?
   * @param callback run in main thread
   */
  fun saveDbByForeground(
    uploadDb: Boolean = true,
    isCreate: Boolean = false,
    needShowLoading: Boolean = true,
    callback: (Int) -> Unit = {}
  ) {
    Timber.d("saveDbByForeground")
    scope.launch(Dispatchers.Main) {
      Timber.d("保存前的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
      if (uploadDb) {
        ForegroundSaveUploadFlow.run(
          needShowLoading = needShowLoading,
          minLoadingMs = MIN_TIME,
          showLoading = { showLoading() },
          dismissLoading = { dismissLoading(it) },
          upload = { onMergeFailed ->
            withContext(Dispatchers.IO) {
              saveAndUploadSnapshot(
                isCreate = isCreate,
                mergeInteractionMode = MergeInteractionMode.FOREGROUND,
                onMergeFailed = onMergeFailed
              )
            }
          },
          callback = { code ->
            callback(code)
          },
          emitSave = {
            entryStateChangeFlow.emit(EntryStateChangeEvent(SAVE))
          }
        )
        return@launch
      }
      ForegroundSaveUploadFlow.runLocalSave(
        needShowLoading = needShowLoading,
        minLoadingMs = MIN_TIME,
        showLoading = { showLoading() },
        dismissLoading = { dismissLoading(it) },
        save = { saveDbAwait() },
        callback = callback,
        emitSave = {
          entryStateChangeFlow.emit(EntryStateChangeEvent(SAVE))
        }
      )
    }
  }

  override fun init(context: Context?) {
  }
}
