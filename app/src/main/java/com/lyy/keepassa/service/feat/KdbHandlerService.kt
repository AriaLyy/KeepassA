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
import com.keepassdroid.database.PwEntry
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
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.setCollection
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
  }

  private var scope = MainScope()
  private var collectionNum = AtomicInteger(0)
  val saveStateFlow = MutableSharedFlow<Int>()
  val collectionStateFlow = MutableStateFlow(CollectionEvent())
  private val collectionEntries = hashSetOf<PwEntryV4>()

  private val loadingDialog: LoadingDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getLoadingDialog()
  }

  fun getCollectionEntries() = collectionEntries
  fun getCollectionNum() = collectionNum.get()

  internal fun updateCollectionEntries(collectionEntries: Set<PwEntryV4>) {
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
    scope.launch {
      if (loadingDialog.isVisible) {
        return@launch
      }
      loadingDialog.show()
    }
  }

  private fun dismissLoading(delay: Long = 0) {
    scope.launch {
      loadingDialog.dismiss(delay)
    }
  }

  /**
   * delete group
   * @param save true: delete that group and save it
   */
  fun deleteGroup(pwGroup: PwGroup, save: Boolean = false) {
    if (BaseApp.isV4) {
      if (BaseApp.KDB!!.pm.canRecycle(pwGroup)) {
        (BaseApp.KDB!!.pm as PwDatabaseV4).recycle(pwGroup as PwGroupV4)
      } else {
        KDBHandlerHelper.getInstance(BaseApp.APP).deleteGroup(BaseApp.KDB, pwGroup, save)
      }
    } else {
      KDBHandlerHelper.getInstance(BaseApp.APP).deleteGroup(BaseApp.KDB, pwGroup, save)
    }
  }

  /**
   * delete entry
   * @param save true: delete that entry and save it
   */
  fun deleteEntry(entry: PwEntry, save: Boolean = false) {
    if (BaseApp.isV4) {
      val v4Entry = entry as PwEntryV4
      if (BaseApp.KDB!!.pm.canRecycle(v4Entry)) {
        BaseApp.KDB!!.pm.recycle(v4Entry)
      } else {
        KDBHandlerHelper.getInstance(BaseApp.APP).deleteEntry(BaseApp.KDB, entry, save)
      }
    } else {
      KDBHandlerHelper.getInstance(BaseApp.APP).deleteEntry(BaseApp.KDB, entry, save)
    }
  }

  /**
   * create new Group
   *
   * @param icon default icon
   * @param customIcon custom icon
   * @param parent 父组，如果是想添加到跟目录，设置null
   */
  fun createGroup(
    groupName: String,
    icon: PwIconStandard,
    customIcon: PwIconCustom?,
    parent: PwGroup
  ): PwGroupV4 {
    val pm: PwDatabase = BaseApp.KDB.pm

    val group = pm.createGroup() as PwGroupV4
    group.initNewGroup(groupName, pm.newGroupId())
    group.icon = icon
    customIcon?.let { group.customIcon = it }
    pm.addGroupTo(group, parent)
    saveDbByBackground()
    return group
  }

  /**
   * add new group
   */
  fun addGroup(group: PwGroup) {
    KDBHandlerHelper.getInstance(BaseApp.APP)
      .createGroup(BaseApp.KDB, group.name, group.icon, group.parent)
  }

  /**
   * add new entry
   */
  fun addEntry(entry: PwEntry) {
    BaseApp.KDB!!.pm.addEntryTo(entry, entry.parent)
  }

  suspend fun saveOnly(needShowLoading: Boolean = false): Int {
    return withContext(Dispatchers.IO) {
      if (needShowLoading) {
        showLoading()
      }

      val b = KDBHandlerHelper.getInstance(BaseApp.APP).save(BaseApp.KDB)
      if (needShowLoading) {
        dismissLoading()
      }
      return@withContext if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
    }
  }

  /**
   * save db by background
   * @param uploadDb true: upload db to cloud
   * @param callback run in main thread
   */
  fun saveDbByBackground(uploadDb: Boolean = false, callback: (Int) -> Unit = {}) {
    Timber.d("start save db by background")
    scope.launch(Dispatchers.IO) {
      val b = KDBHandlerHelper.getInstance(BaseApp.APP).save(BaseApp.KDB)
      Timber.d("保存后的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
      if (uploadDb) {
        val response = DbSynUtil.uploadSyn(BaseApp.dbRecord!!, false)
        Timber.i(response.msg)

        withContext(Dispatchers.Main) {
          saveStateFlow.emit(response.code)
          callback.invoke(response.code)
        }
        return@launch
      }
      val code = if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
      withContext(Dispatchers.Main) {
        saveStateFlow.emit(code)
        callback.invoke(code)
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
    scope.launch(Dispatchers.Main) {
      Timber.d("保存前的数据库hash：${BaseApp.KDB.hashCode()}，num = ${BaseApp.KDB!!.pm.entries.size}")
      val b = withContext(Dispatchers.IO) {
        return@withContext KDBHandlerHelper.getInstance(BaseApp.APP).save(BaseApp.KDB)
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
          dismissLoading(if ((endTime - startTime) < MIN_TIME) 0L else MIN_TIME)
        }
        saveStateFlow.emit(response.code)
        callback.invoke(response.code)
        return@launch
      }
      val code = if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_SAVE_DB_FAIL
      saveStateFlow.emit(code)
      callback.invoke(code)
    }
  }

  override fun init(context: Context?) {
  }
}