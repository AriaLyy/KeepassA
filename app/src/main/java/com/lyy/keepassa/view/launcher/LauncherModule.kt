/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.keepassdroid.utils.UriUtil
import com.lahm.library.EasyProtectorLib
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.QuickUnLockRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.FingerprintUtil
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.util.isAFS
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.widget.BubbleTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class LauncherModule : BaseModule() {
  private val itemData: MutableLiveData<List<SimpleItemEntity>> = MutableLiveData()

  /**
   * 安全检查
   */
  fun securityCheck(context: Context) {
    val needCheckEnv = PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.resources.getString(R.string.set_key_need_root_check), true)
    if (!needCheckEnv) {
      return
    }

    val resources = context.resources
    if (EasyProtectorLib.checkIsRoot()) {
      val vector = VectorDrawableCompat.create(resources, R.drawable.ic_eco, context.theme)
      vector?.setTint(ResUtil.getColor(R.color.red))
      val dialog = MsgDialog.generate {
        msgTitle = resources.getString(R.string.warning)
        msgContent = resources.getString(R.string.warning_rooted)
        msgTitleEndIcon = vector
        showCancelBt = false
        build()
      }
      dialog.show()
    }
  }

  /**
   * 获取快速解锁记录
   */
  fun getQuickUnlockRecord(dbUri: String) = liveData {
    var record: QuickUnLockRecord? = null
    withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      record = dao.findRecord(dbUri)
    }
    emit(record)
  }

  /**
   * 删除指纹解锁记录
   */
  fun deleteFingerprint(dbUri: String) {
    if (!FingerprintUtil.hasBiometricPrompt(BaseApp.APP)) {
      return
    }
    viewModelScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      val record = dao.findRecord(dbUri)
      PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
          .edit {
            putBoolean(BaseApp.APP.getString(R.string.set_quick_unlock), false)
            commit()
          }
      if (record != null) {
        dao.deleteRecord(record)
      }
    }
  }

  /**
   * 检查是否需要启动指纹解锁
   */
  fun isNeedUseFingerprint(dbUri: String) = liveData {
    if (!FingerprintUtil.hasBiometricPrompt(BaseApp.APP)) {
      emit(false)
      return@liveData
    }

    val needOpen = withContext(Dispatchers.IO) {
      val unlockDao = BaseApp.appDatabase.quickUnlockDao()
      val unLockRecord = unlockDao.findRecord(dbUri)
      if (unLockRecord == null) {
        Log.d(TAG, "unLockRecord is null")
        return@withContext false
      }
      Log.d(TAG, "is full unlock = ${unLockRecord.isFullUnlock}")
      val dbDao = BaseApp.appDatabase.dbRecordDao()
      val dbRecord = dbDao.findRecord(dbUri)
      if (dbRecord == null) {
        Log.d(TAG, "dbRecord is null")
        return@withContext false
      }

      return@withContext unLockRecord.isFullUnlock
    }
    emit(needOpen)
  }

  /**
   * 打开数据库
   *
   */
  fun openDb(
    context: Context,
    record: DbRecord,
    dbPass: String
  ) = liveData {
    val db: Database? = withContext(Dispatchers.IO) {
      var temp: Database? = null
      try {
        temp = when (record.getDbPathType()) {
          AFS -> {
            openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
          }
          DROPBOX -> {
            openDropboxDb(context, record, dbPass)
          }
          WEBDAV -> {
            openWebDavDb(context, record, dbPass)
          }
          else -> null
        }
      } catch (e: Exception) {
        HitUtil.toaskOpenDbException(e)
        e.printStackTrace()
      }
      temp
    }
    emit(db)
  }

  /**
   * 打开坚果云数据
   */
  private suspend fun openWebDavDb(
    context: Context,
    record: DbRecord,
    dbPass: String
  ): Database? {
    val dao = BaseApp.appDatabase.cloudServiceInfoDao()
    val serviceInfo = dao.queryServiceInfo(record.cloudDiskPath!!)
    if (serviceInfo == null) {
      HitUtil.toaskShort(context.getString(R.string.invalid_auth))
      return null
    }
    WebDavUtil.login(
        serviceInfo.cloudPath, QuickUnLockUtil.decryption(serviceInfo.userName),
        QuickUnLockUtil.decryption(serviceInfo.password)
    )

    val cacheFile = record.getDbUri()
        .toFile()
    if (cacheFile.exists()
        && DbSynUtil.serviceModifyTime == DbSynUtil.getFileServiceModifyTime(record)
    ) {
      Log.i(TAG, "文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
    val cachePath = DbSynUtil.downloadOnly(context, record, Uri.fromFile(cacheFile))
    if (TextUtils.isEmpty(cachePath)) {
      return null
    } else {
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
  }

  /**
   * 打开dropbox的数据库
   */
  private suspend fun openDropboxDb(
    context: Context,
    record: DbRecord,
    dbPass: String
  ): Database? {
    val cacheFile = record.getDbUri()
        .toFile()
    if (cacheFile.exists()
        && DbSynUtil.serviceModifyTime == DbSynUtil.getFileServiceModifyTime(record)
    ) {
      Log.i(TAG, "文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
    val cachePath = DbSynUtil.downloadOnly(context, record, Uri.fromFile(cacheFile))
    if (TextUtils.isEmpty(cachePath)) {
      return null
    } else {
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
  }

  /**
   * 打开数据库文件
   * @param dbUri 如果是AFS，dbUri表示本地文件的Uri；如果是云端文件，表示的是云端文件的路径
   */
  private suspend fun openDbFile(
    context: Context,
    dbUri: Uri,
    dbPass: String,
    keyUri: Uri?,
    record: DbRecord
  ): Database? {
    val db = KDBHandlerHelper.getInstance(context)
        .openDb(dbUri, dbPass, keyUri)
    if (db != null) {
      val dbName = UriUtil.getFileNameFromUri(context, dbUri)
      BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
      KeepassAUtil.subShortPass()
      if (keyUri != null) {
        BaseApp.dbKeyPath = QuickUnLockUtil.encryptStr(keyUri.toString())
      }
      //              BaseApp.KDB?.clear(context)
      // 保存打开记录
      BaseApp.KDB = db
      BaseApp.dbName = db.pm.name
      BaseApp.dbFileName = dbName

      BaseApp.dbVersion = "Keepass ${if (PwDatabase.isKDBExtension(dbName)) "3.x" else "4.x"}"
      BaseApp.isV4 = !PwDatabase.isKDBExtension(dbName)
      BaseApp.dbRecord = record
      KeepassAUtil.saveLastOpenDbHistory(record)

      if (!BaseApp.isAFS()) {
        DbSynUtil.updateServiceModifyTime(record)
      }
    }
    return db
  }

  /**
   * 数据库打开方式
   */
  fun getDbOpenTypeData(context: Context): LiveData<List<SimpleItemEntity>>? {

    val titles = context.resources.getStringArray(R.array.main_items)
    val icons = context.resources.obtainTypedArray(R.array.main_item_icons)
    val types = context.resources.getIntArray(R.array.main_item_types)

    val items = ArrayList<SimpleItemEntity>()
    for ((index, title) in titles.withIndex()) {
      val item = SimpleItemEntity()
      item.title = title
      item.id = types[index]
      item.icon = icons.getResourceId(index, 0)
      items.add(item)
    }
    icons.recycle()
    itemData.postValue(items)
    return itemData
  }

  /**
   * 获取上一次打开的数据信息
   * 对于afs的文件如果uri失效，则删除该uri
   */
  fun getLastOpenDbHistory(context: Context) = liveData {
    val data = withContext(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.dbRecordDao()
      val records = dao.getAllRecord()
      var newRecord: DbRecord? = null
      val needRemoveRecords = ArrayList<DbRecord>()
      for (record in records) {
        val localUri = Uri.parse(record.localDbUri)
        if (!UriUtil.checkPermissions(context, localUri) && record.isAFS()) {
          needRemoveRecords.add(record)
          continue
        }
        newRecord = record
        break
      }

      for (record in needRemoveRecords) {
        dao.deleteRecord(record)
      }

      newRecord
    }
    emit(data)
  }

}