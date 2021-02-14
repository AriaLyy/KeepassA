/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.launcher

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.core.content.edit
import androidx.core.net.toFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arialyy.frame.util.KeyStoreUtil
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
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.util.isAFS
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.dialog.MsgDialog
import com.tencent.bugly.crashreport.BuglyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherModule : BaseModule() {
  private val itemData: MutableLiveData<List<SimpleItemEntity>> = MutableLiveData()
  private val unlockEvent = MutableLiveData<Pair<Boolean, String?>>()
  private val scope = MainScope()

  override fun onCleared() {
    super.onCleared()
    scope.cancel()
  }

  private val keyStoreUtil by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreUtil()
    } else {
      null
    }
  }

  /**
   * 显示指纹解锁
   */
  @SuppressLint("RestrictedApi")
  @TargetApi(Build.VERSION_CODES.M)
  fun showBiometricPrompt(
    fragment: OpenDbFragment,
    quickUnlockRecord: QuickUnLockRecord?,
    openDbRecord: DbRecord
  ) {
    if (!fragment.isAdded) {
      return
    }
    if (quickUnlockRecord == null) {
      KLog.e(TAG, "解锁记录为空")
      return
    }

    val resource = fragment.requireContext().resources
    val promptInfo =
      BiometricPrompt.PromptInfo.Builder()
          .setTitle(resource.getString(R.string.fingerprint_unlock))
          .setSubtitle(resource.getString(R.string.verify_finger))
          .setNegativeButtonText(resource.getString(R.string.cancel))
          //        .setConfirmationRequired(false)
          .build()

    val biometricPrompt = BiometricPrompt(fragment,
        ArchTaskExecutor.getMainThreadExecutor(),
        object : AuthenticationCallback() {
          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence
          ) {
            if (!fragment.isAdded) {
              KLog.e(TAG, "Fragment没有被加载")
              return
            }
            val str = if (errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON) {
              "${resource.getString(R.string.verify_finger)}${resource.getString(R.string.cancel)}"
            } else {
              resource.getString(R.string.verify_finger_fail)
            }
            HitUtil.snackShort(fragment.getRootView(), str)
            unlockEvent.postValue(Pair(false, null))
          }

          override fun onAuthenticationSucceeded(result: AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            try {
              val auth: CryptoObject? = result.cryptoObject
              val cipher = auth!!.cipher!!
              val pass = QuickUnLockUtil.decryption(
                  keyStoreUtil?.decryptData(
                      cipher, quickUnlockRecord.dbPass
                  )
              )
              unlockEvent.postValue(Pair(true, pass))
            } catch (e: Exception) {
              e.printStackTrace()
              deleteBiomKey(fragment, openDbRecord)
            }
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (fragment.isAdded) {
              HitUtil.snackShort(
                  fragment.getRootView(),
                  resource.getString(R.string.verify_finger_fail)
              )
            }
            unlockEvent.postValue(Pair(false, null))
          }
        })
    try {
      // Displays the "log in" prompt.
      keyStoreUtil?.let {
        biometricPrompt.authenticate(
            promptInfo, CryptoObject(it.getDecryptCipher(quickUnlockRecord.passIv))
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      deleteBiomKey(fragment, openDbRecord)
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun deleteBiomKey(
    fragment: OpenDbFragment,
    openDbRecord: DbRecord
  ) {
    if (!fragment.isAdded) {
      BuglyLog.d(TAG, "deleteBiomKey fragment isAdded = false")
      return
    }
    val resource = fragment.requireContext().resources
    keyStoreUtil?.deleteKeyStore()
    HitUtil.snackLong(fragment.getRootView(), resource.getString(R.string.hint_fingerprint_modify))
    fragment.hideFingerprint()
    deleteFingerprint(openDbRecord.localDbUri)
  }

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
  fun getQuickUnlockRecord(
    openDbRecord: DbRecord,
    fragment: OpenDbFragment
  ): MutableLiveData<Pair<Boolean, String?>> {
    scope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.quickUnlockDao()
      val record: QuickUnLockRecord? = dao.findRecord(openDbRecord.localDbUri)
      withContext(Dispatchers.Main) {
        showBiometricPrompt(fragment, record, openDbRecord)
      }
    }
    return unlockEvent
  }

  /**
   * 删除指纹解锁记录
   */
  private fun deleteFingerprint(dbUri: String) {
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
        KLog.d(TAG, "unLockRecord is null")
        return@withContext false
      }
      KLog.d(TAG, "is full unlock = ${unLockRecord.isFullUnlock}")
      val dbDao = BaseApp.appDatabase.dbRecordDao()
      val dbRecord = dbDao.findRecord(dbUri)
      if (dbRecord == null) {
        KLog.d(TAG, "dbRecord is null")
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
    KLog.d(TAG, "打开数据库")
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
      KLog.i(TAG, "文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
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
    record: DbRecord,
    dbPass: String
  ): Database? {
    val cacheFile = record.getDbUri()
        .toFile()
    if (cacheFile.exists()
        && DbSynUtil.serviceModifyTime == DbSynUtil.getFileServiceModifyTime(record)
    ) {
      KLog.i(TAG, "文件存在，并且云端文件时间和本地保存的时间一致，不会重新从云端下载数据库")
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
    record: DbRecord
  ): Database? {
    try {
      val db = KDBHandlerHelper.getInstance(context)
          .openDb(dbUri, dbPass, keyUri)
      if (db != null) {
        val dbName = UriUtil.getFileNameFromUri(context, dbUri)
        BaseApp.dbPass = QuickUnLockUtil.encryptStr(dbPass)
        KeepassAUtil.instance.subShortPass()
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
        KeepassAUtil.instance.saveLastOpenDbHistory(record)

        if (!BaseApp.isAFS()) {
          DbSynUtil.updateServiceModifyTime(record)
        }
      }
      return db
    } catch (e: Exception) {
      HitUtil.toaskOpenDbException(e)
      e.printStackTrace()
    }
    return null
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