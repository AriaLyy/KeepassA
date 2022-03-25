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
import com.keepassdroid.database.helper.CreateDBHelper
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.UNKNOWN
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2:03 下午 2022/3/24
 **/
@Route(path = "/service/kdb")
class KdbService : IProvider {
  companion object {
    const val MIN_TIME = 200L
  }

  var scope = MainScope()
  val saveStateFlow = MutableSharedFlow<Int>()

  private val loadingDialog: LoadingDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getLoadingDialog()
  }

  private fun showLoading() {
    if (loadingDialog.isVisible) {
      return
    }
    loadingDialog.show()
  }

  /**
   * 创建数据库时创建默认的群组
   */
  private fun createDefaultGroup(context: Context) {
    val icons = arrayListOf(25, 47, 66, 62, 43)
    val names = context.resources.getStringArray(R.array.create_normal_group)
    val pm: PwDatabase = BaseApp.KDB.pm
    for ((index, name) in names.withIndex()) {
      val group = pm.createGroup() as PwGroupV4
      group.initNewGroup(name, pm.newGroupId())
      group.icon = PwIconStandard(icons[index]) // 需要设置默认图标

      // 处理回收站
      if (icons[index] == 43) {
        group.enableAutoType = false
        group.enableSearching = false
        group.isExpanded = false
        (BaseApp.KDB.pm as PwDatabaseV4).recycleBinUUID = group.uuid
      }
      pm.addGroupTo(group, pm.rootGroup)
    }
  }

  /**
   * create db
   * @param keyUri the db key
   * @param cloudPath cloud path eg: https://dev.jianguo.com
   * @param storageType [StorageType]
   */
  fun createDb(
    dbName: String,
    localDbUri: Uri?,
    dbPass: String,
    keyUri: Uri?,
    cloudPath: String?,
    storageType: StorageType = UNKNOWN,
    callback: (Int) -> Unit
  ) {
    val context = BaseApp.APP
    showLoading()
    scope.launch(Dispatchers.IO) {
      try {
        // 创建db
        val cdb = CreateDBHelper(context, dbName, localDbUri)
        if (keyUri != null) {
          cdb.setKeyFile(keyUri)
          BaseApp.dbKeyPath = QuickUnLockUtil.encryptStr(keyUri.toString())
        }
        cdb.setPass(dbPass, null)
        val db = cdb.build()

        // 保存打开记录
        BaseApp.KDB = db
        BaseApp.dbName = db.pm.name
        BaseApp.dbFileName = dbName
        BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
        KeepassAUtil.instance.subShortPass()

        // 创建默认群组
        createDefaultGroup(context)

        BaseApp.dbVersion = "Keepass ${if (PwDatabase.isKDBExtension(dbName)) "3.x" else "4.x"}"
        BaseApp.isV4 = !PwDatabase.isKDBExtension(dbName)
        val record = DbHistoryRecord(
          time = System.currentTimeMillis(),
          type = storageType.name,
          localDbUri = localDbUri.toString(),
          cloudDiskPath = cloudPath,
          keyUri = keyUri?.toString() ?: "",
          dbName = dbName
        )
        BaseApp.dbRecord = record
        BaseApp.isLocked = false

        // 保存并上传数据库到云端
        saveDbByForeground(uploadDb = true, isCreate = true, callback)
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
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

  suspend fun saveOnly(): Int {
    return withContext(Dispatchers.IO) {
      val b = KDBHandlerHelper.getInstance(BaseApp.APP).save(BaseApp.KDB)
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
   *  @param callback run in main thread
   */
  fun saveDbByForeground(
    uploadDb: Boolean = true,
    isCreate: Boolean = false,
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
        showLoading()
        val response = DbSynUtil.uploadSyn(BaseApp.dbRecord!!, isCreate)
        Timber.i(response.msg)
        val endTime = System.currentTimeMillis()
        loadingDialog.dismiss(if ((endTime - startTime) < MIN_TIME) 0L else MIN_TIME)
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