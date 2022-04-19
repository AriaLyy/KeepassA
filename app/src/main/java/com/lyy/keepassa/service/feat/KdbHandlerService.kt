/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
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
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.event.CollectionEvent
import com.lyy.keepassa.event.CollectionEventType
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_ADD
import com.lyy.keepassa.event.CollectionEventType.COLLECTION_STATE_REMOVE
import com.lyy.keepassa.event.EntryState.CREATE
import com.lyy.keepassa.event.EntryState.DELETE
import com.lyy.keepassa.event.EntryState.MODIFY
import com.lyy.keepassa.event.EntryState.MOVE
import com.lyy.keepassa.event.EntryStateChangeEvent
import com.lyy.keepassa.event.GroupStateChangeEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil.isNull
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.setCollection
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:03 下午 2022/3/24
 **/
@Route(path = "/service/kdbHandler")
class KdbHandlerService : IProvider {
  companion object {
    const val MIN_TIME = 200L
    private const val NEED_SAVE_BY_REAL_TIME = false
  }

  private var scope = MainScope()
  private var collectionNum = AtomicInteger(0)

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
          kdbHelper.deleteGroup(BaseApp.KDB, pwGroup, NEED_SAVE_BY_REAL_TIME)
        }
      }
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
      if (NEED_SAVE_BY_REAL_TIME) {
        withContext(Dispatchers.IO) {
          if (kdbHelper.save(BaseApp.KDB)) {
            val parent = v4Entry.parent
            // Mark parent dirty
            if (parent != null) {
              BaseApp.KDB.dirty.add(parent)
            }
            return@withContext
          }
        }
      }

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

        if (NEED_SAVE_BY_REAL_TIME) {
          if (kdbHelper.save(BaseApp.KDB)) {
            val parent = v4Entry.parent
            // Mark parent dirty
            if (parent != null) {
              BaseApp.KDB.dirty.add(parent)
            }
            return@withContext
          }
          BaseApp.KDB.pm.removeEntryFrom(v4Entry, targetParent)
        }
      }
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
        kdbHelper.deleteEntry(BaseApp.KDB, v4Entry, NEED_SAVE_BY_REAL_TIME)
      }
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
        if (NEED_SAVE_BY_REAL_TIME) {
          if (kdbHelper.save(BaseApp.KDB)) {
            BaseApp.KDB.dirty.add(self.parent)
          }
        }
      }

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

        if (NEED_SAVE_BY_REAL_TIME) {
          if (kdbHelper.save(BaseApp.KDB)) {
            BaseApp.KDB.dirty.add(parent)
          } else {
            pm.removeGroupFrom(group, parent)
          }
        }
        return@withContext group
      }
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
  }

  fun addGroup(group: PwGroupV4){
    BaseApp.KDB?.pm?.addGroupTo(group, group.parent)
  }

  /**
   * add new entry
   */
  fun addEntry(entry: PwEntryV4) {
    if (NEED_SAVE_BY_REAL_TIME) {
      kdbHelper.saveEntry(BaseApp.KDB, entry)
    } else {
      BaseApp.KDB!!.pm.addEntryTo(entry, entry.parent)
    }
    scope.launch {
      entryStateChangeFlow.emit(EntryStateChangeEvent(CREATE, entry))
    }
  }

  /**
   * only add entry
   */
  fun addEntryTo(entry: PwEntryV4, parent: PwGroup) {
    BaseApp.KDB!!.pm.addEntryTo(entry, parent)
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
        callback.invoke(if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL)
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
      return
    }
    if (BaseApp.isLocked){
      Timber.d("db is locked")
      return
    }
    scope.launch {
      mutex.withLock {
        withContext(Dispatchers.IO) {
          val b = kdbHelper.save(BaseApp.KDB)
          Timber.d("保存后的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
          delay(1000)
          if (uploadDb) {
            val response = DbSynUtil.uploadSyn(BaseApp.dbRecord!!, false)
            Timber.i(response.msg)

            withContext(Dispatchers.Main) {
              callback.invoke(response.code)
            }
            return@withContext
          }
          val code = if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
          withContext(Dispatchers.Main) {
            callback.invoke(code)
          }
        }
      }
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
      mutex.withLock {
        Timber.d("保存前的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
        val b = withContext(Dispatchers.IO) {
          return@withContext kdbHelper.save(BaseApp.KDB)
        }
        Timber.d("保存后的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
        if (uploadDb) {
          val startTime = System.currentTimeMillis()
          if (needShowLoading) {
            showLoading()
          }
          val response = withContext(Dispatchers.IO) {
            return@withContext DbSynUtil.uploadSyn(BaseApp.dbRecord!!, isCreate)
          }
          Timber.i(response.msg)
          val endTime = System.currentTimeMillis()
          if (needShowLoading) {
            dismissLoading(if ((endTime - startTime) < MIN_TIME) MIN_TIME else 0L)
          }
          callback.invoke(response.code)
          return@launch
        }
        val code = if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
        callback.invoke(code)
      }
    }
  }

  override fun init(context: Context?) {
  }
}