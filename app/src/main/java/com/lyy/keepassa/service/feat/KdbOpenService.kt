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
import android.text.TextUtils
import androidx.core.net.toFile
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.arialyy.frame.router.Routerfit
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.helper.CreateDBHelper
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.OneDriveUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.util.isCollection
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.AFS
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.StorageType.UNKNOWN
import com.lyy.keepassa.view.StorageType.WEBDAV
import com.lyy.keepassa.view.dialog.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:25 上午 2022/3/28
 **/

@Route(path = "/service/kdbOpen")
class KdbOpenService : IProvider {
  private var scope = MainScope()
  val openDbFlow = MutableSharedFlow<Database?>(0)

  private val loadingDialog: LoadingDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getLoadingDialog()
  }

  private fun showLoading() {
    scope.launch {
      if (loadingDialog.isVisible) {
        return@launch
      }
      loadingDialog.show()
    }
  }

  private fun dismissLoading() {
    scope.launch {
      loadingDialog.dismiss()
    }
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
    storageType: StorageType = UNKNOWN
  ) {
    val context = BaseApp.APP
    scope.launch(Dispatchers.IO) {
      try {
        showLoading()
        // 创建db
        val cdb = CreateDBHelper(context, dbName, localDbUri)
        if (keyUri != null) {
          cdb.setKeyFile(keyUri)
          BaseApp.dbKeyPath = QuickUnLockUtil.encryptStr(keyUri.toString())
        }
        cdb.setPass(dbPass, null)
        val db = cdb.build()

        val success = checkCreatedDb(BaseApp.APP, localDbUri!!, dbPass, keyUri)
        if (!success){
          scope.launch {
            openDbFlow.emit(null)
          }
          Timber.e("create db fail")
          return@launch
        }

        // 保存打开记录
        BaseApp.KDB = db
        BaseApp.dbName = db.pm.name
        BaseApp.dbFileName = dbName
        BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
        KpaUtil.setEmptyPass(dbPass.isEmpty())
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
        KpaUtil.kdbHandlerService.saveDbByForeground(
          uploadDb = true,
          isCreate = true,
          needShowLoading = false
        ) {
          NotificationUtil.startDbOpenNotify(context)
          dismissLoading()
          scope.launch {
            openDbFlow.emit(BaseApp.KDB)
          }
        }
      } catch (e: Exception) {
        HitUtil.toaskOpenDbException(e)
        scope.launch {
          openDbFlow.emit(null)
        }
        Timber.e(e)
      }
    }
  }

  /**
   * check db
   */
  private fun checkCreatedDb(
    context: Context,
    dbUri: Uri,
    dbPass: String,
    keyUri: Uri?
  ): Boolean {
    return KDBHandlerHelper.getInstance(context)
      .openDb(dbUri, dbPass, keyUri) != null
  }

  /**
   * open database
   * @param needShowLoading Do you need to display the load dialog box?
   */
  fun openDb(
    context: Context,
    record: DbHistoryRecord,
    dbPass: String,
    needShowLoading: Boolean = true
  ) {
    Timber.d("打开数据库")
    if (needShowLoading) {
      showLoading()
    }
    scope.launch {
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
            ONE_DRIVE -> {
              openOneDriveDb(context, record, dbPass)
            }
            else -> null
          }
        } catch (e: Exception) {
          HitUtil.toaskOpenDbException(e)
          Timber.e(e)
        }
        temp
      }

      if (db != null) {
        BaseApp.isLocked = false
        BaseApp.KDB = db
        NotificationUtil.startDbOpenNotify(context)
        withContext(Dispatchers.IO) {
          var collectionNum = 0
          val entrySet = hashSetOf<PwEntryV4>()
          BaseApp.KDB.pm.entries.forEach {
            if ((it.value as PwEntryV4).isCollection()) {
              entrySet.add(it.value as PwEntryV4)
              collectionNum++
            }
          }
          KpaUtil.kdbHandlerService.updateCollectionNum(collectionNum)
          KpaUtil.kdbHandlerService.updateCollectionEntries(entrySet)
        }
      }

      if (needShowLoading) {
        dismissLoading()
      }
      openDbFlow.emit(db)
    }
  }

  /**
   * 打开OneDrive数据库
   */
  private suspend fun openOneDriveDb(
    context: Context,
    record: DbHistoryRecord,
    dbPass: String
  ): Database? {
    val channel = Channel<Database?>()
    var db: Database? = null

    OneDriveUtil.initOneDrive {
      if (!it) {
        scope.launch {
          channel.send(null)
        }
        return@initOneDrive
      }
      OneDriveUtil.loginCallback = object : OneDriveUtil.OnLoginCallback {
        override fun callback(success: Boolean) {
          scope.launch {
            if (!success) {
              channel.send(null)
              return@launch
            }
            val cacheFile = record.getDbUri()
              .toFile()
            val cloudFileInfo = OneDriveUtil.getFileInfo(record.cloudDiskPath!!)
            if (cacheFile.exists()
              && cloudFileInfo != null
              && OneDriveUtil.checkContentHash(cloudFileInfo.contentHash, record.getDbUri())
            ) {
              Timber.i("文件存在，并且hash一致，将使用本地数据库")
              db = openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
              channel.send(db)
              return@launch
            }
            val cachePath = DbSynUtil.downloadOnly(context, record, Uri.fromFile(cacheFile))
            db = if (TextUtils.isEmpty(cachePath)) {
              null
            } else {
              openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
            }
            channel.send(db)
          }
        }
      }
      OneDriveUtil.loadAccount()
    }
    repeat(1) {
      db = channel.receive()
    }

    return db
  }

  /**
   * 打开坚果云数据
   */
  private suspend fun openWebDavDb(
    context: Context,
    record: DbHistoryRecord,
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
    val cloudFileInfo = DbSynUtil.getFileInfo(record)
    if (cacheFile.exists()
      && (cloudFileInfo == null || DbSynUtil.serviceModifyTime == cloudFileInfo.serviceModifyDate)
    ) {
      Timber.i("文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
    val cachePath = DbSynUtil.downloadOnly(context, record, Uri.fromFile(cacheFile))
    return if (TextUtils.isEmpty(cachePath)) {
      null
    } else {
      openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
  }

  /**
   * 打开dropbox的数据库
   */
  private suspend fun openDropboxDb(
    context: Context,
    record: DbHistoryRecord,
    dbPass: String
  ): Database? {
    val cacheFile = record.getDbUri()
      .toFile()
    if (cacheFile.exists()
      && DbSynUtil.serviceModifyTime == DbSynUtil.getFileServiceModifyTime(record)
    ) {
      Timber.i("文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
      return openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
    }
    val cachePath = DbSynUtil.downloadOnly(context, record, Uri.fromFile(cacheFile))
    return if (TextUtils.isEmpty(cachePath)) {
      null
    } else {
      openDbFile(context, record.getDbUri(), dbPass, record.getDbKeyUri(), record)
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
    record: DbHistoryRecord
  ): Database? {
    try {
      val db = KDBHandlerHelper.getInstance(context)
        .openDb(dbUri, dbPass, keyUri)
      if (db != null) {
        val dbName = UriUtil.getFileNameFromUri(context, dbUri)
        KpaUtil.setEmptyPass(dbPass.isEmpty())
        BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
        KeepassAUtil.instance.subShortPass()
        if (keyUri != null) {
          BaseApp.dbKeyPath = QuickUnLockUtil.encryptStr(keyUri.toString())
        } else {
          BaseApp.dbKeyPath = null
        }
        //              BaseApp.KDB?.clear(context)
        // 保存打开记录
        BaseApp.KDB = db
        BaseApp.dbName = db.pm.name
        BaseApp.dbFileName = dbName

        BaseApp.dbVersion = "Keepass ${if (PwDatabase.isKDBExtension(dbName)) "3.x" else "4.x"}"
        BaseApp.isV4 = !PwDatabase.isKDBExtension(dbName)
        BaseApp.dbRecord = record
        KeepassAUtil.instance.saveLastOpenDbHistory(record)

        if (!BaseApp.isAFS()) {
          DbSynUtil.updateServiceModifyTime(record)
        }
      }
      return db
    } catch (e: Exception) {
      HitUtil.toaskOpenDbException(e)
      Timber.e(e)
    }
    return null
  }

  override fun init(context: Context?) {
  }
}