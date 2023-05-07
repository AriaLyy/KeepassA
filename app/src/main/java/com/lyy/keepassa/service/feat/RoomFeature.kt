/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.feat

import android.content.Context
import androidx.room.Room
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.DbMigration
import com.lyy.keepassa.dao.AppDatabase
import com.lyy.keepassa.util.QuickUnLockUtil
import com.tencent.wcdb.database.SQLiteCipherSpec
import com.tencent.wcdb.room.db.WCDBOpenHelperFactory

object RoomFeature :IFeature{
  override fun init(context: Context) {
    // 初始化数据库
    val cipherSpec = SQLiteCipherSpec() // 指定加密方式，使用默认加密可以省略
      .setPageSize(4096)
      .setKDFIteration(64000)
    val factory = WCDBOpenHelperFactory()
      .passphrase(QuickUnLockUtil.getDbPass().toByteArray()) // 指定加密DB密钥，非加密DB去掉此行
      .cipherSpec(cipherSpec) // 指定加密方式，使用默认加密可以省略
      .writeAheadLoggingEnabled(true) // 打开WAL以及读写并发，可以省略让Room决定是否要打开
      .asyncCheckpointEnabled(true) // 打开异步Checkpoint优化，不需要可以省略
    BaseApp.appDatabase = Room.databaseBuilder(
      context,
      AppDatabase::class.java, AppDatabase.DB_NAME
    )
      .openHelperFactory(factory)
      .addMigrations(DbMigration.MIGRATION_2_3(), DbMigration.MIGRATION_3_4())
      .build()
  }
}