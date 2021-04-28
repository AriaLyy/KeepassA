/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.liveData
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.helper.CreateDBHelper
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.UNKNOWN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateDbModule : BaseModule() {

  /**
   * 设置的数据库密码
   */
  var dbPass: String = ""

  /**
   * 数据库名，包含.kdbx
   */
  var dbName: String = ""

  /**
   * 数据库uri
   */
  var localDbUri: Uri? = null

  /**
   * key uri
   */
  var keyUri: Uri? = null

  /**
   * key 的名字
   */
  var keyName: String = ""

  /**
   * 数据库类型
   */
  var dbPathType: DbPathType = UNKNOWN

  /**
   * 云盘路径
   */
  var cloudPath: String = ""

  /**
   * 创建并打开数据库
   */
  fun createAndOpenDb(
    context: Context
  ) = liveData {
    val db: Database? = withContext(Dispatchers.IO) {
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
            type = dbPathType.name,
            localDbUri = localDbUri.toString(),
            cloudDiskPath = cloudPath,
            keyUri = if (keyUri == null) "" else keyUri.toString(),
            dbName = dbName
        )
        BaseApp.dbRecord = record

        // 保存并上传数据库到云端
        val code = KdbUtil.saveDb()

        if (dbPathType == AFS) {
          KeepassAUtil.instance.saveLastOpenDbHistory(record)
        }

        if (code != DbSynUtil.STATE_SUCCEED) {
          return@withContext null
        }

        return@withContext BaseApp.KDB
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext null
    }

    emit(db)
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
   * 数据库打开方式
   */
  fun getDbOpenTypeData(context: Context) = liveData {

    val titles = context.resources.getStringArray(R.array.cloud_names)
    val icons = context.resources.obtainTypedArray(R.array.path_type_img)
    val items = ArrayList<SimpleItemEntity>()
    for ((index, title) in titles.withIndex()) {
      val item = SimpleItemEntity()
      item.title = title
      item.subTitle = titles[index]
      item.id = index
      item.icon = icons.getResourceId(index, 0)
      items.add(item)
    }
    icons.recycle()
    emit(items)
  }

}