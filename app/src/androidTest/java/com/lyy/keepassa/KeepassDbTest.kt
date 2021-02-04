/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepassdroid.Database
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.helper.CreateDBHelper
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.lyy.keepassa.util.QuickUnLockUtil
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class KeepassDbTest {
  private val TAG = "KeepassDbTest"
  private val context: Context = ApplicationProvider.getApplicationContext()

//  private val dbName = "test.kdbx"
//  private val pass = "123456"
//  private val keyName = "db.key"
  private val dbName = "yuyu_pw_db.kdbx"
  private val pass = "Xiaotaiyan123"
  private val keyName = "yuyu_pw_db.key"

  @Test
  fun useAppContext() {
    Log.d(TAG, "pkgName = ${context.packageName}")
    val f = context.filesDir
    Log.d(TAG, "fileName = ${f.name}")
//    EditHelper.getInstance(context)
//        .openDb("test.kdbx", f.path, "123456", "/Users/aria/Downloads/kpa/db.key")
  }

  @Test
  fun createDb() {
    val dir = context.filesDir
    val cdb = CreateDBHelper(context, dbName, Uri.fromFile(File(dir, dbName)))
    cdb.setPass(pass, null)
    cdb.setKeyFile(Uri.fromFile(File(dir, keyName)))
    val db = cdb.build()
    // 创建完成后可以直接读取数据库
    Log.d(TAG, "create db ${if (db != null) "success" else "fail"}")
//    if (db != null) {
//      readDb(db)
//    }
  }

  @Test
  fun openDb() {
    val dir = context.filesDir.path + "/"
    val db = KDBHandlerHelper.getInstance(context)
        .openDb(dbName, dir + dbName, pass, dir + keyName)
//        .openDb(dbName, dir + dbName, pass, null)
    Log.d(TAG, "open db ${if (db != null) "success" else "fail"}")
//    if (db != null) {
//      readDb(db)
//    }
  }

  /**
   * 增加组
   */
  @Test
  fun addGroup() {
//    val db = getDb()
//    val group = KDBHandlerHelper.getInstance(context)
//        .createGroup(db, "test", 45, null)
//    Log.d(TAG, "创建组：${if (group != null) "成功" else "失败"}")
//    readDb(db)
  }

  @Test
  fun addEntry() {
    val db = getDb()
    val entry = PwEntryV4()
  }

  @Test
  fun testCustomData() {
    val dir = context.filesDir
    val cdb = CreateDBHelper(context, dbName, Uri.fromFile(File(dir, dbName)))
    cdb.setPass(pass, null)
    cdb.setKeyFile(Uri.fromFile(File(dir, keyName)))
    val db = cdb.build()
    // 创建自定义数据
    val entryV4 = PwEntryV4()
    entryV4.parent = db.pm.rootGroup as PwGroupV4
    entryV4.setString(PwEntryV4.STR_TITLE, "title", false)
    entryV4.customData["sss"] = "ggggg"
    KDBHandlerHelper.getInstance(context).saveEntry(db, entryV4)
  }

  private fun getDb(): Database {
    val dir = context.filesDir.path + "/"
    return KDBHandlerHelper.getInstance(context)
        .openDb(dbName, dir + dbName, pass, dir + keyName)
  }

  /**
   * 读取数据库信息
   */
  private fun readDb(db: Database) {
    val pm = db.pm
    for (i in pm.groups) {
      readGroup(i.key, i.value)
    }
    for (i in pm.entries) {
      readEntry(i.value)
    }
  }

  /**
   * 读取组信息
   */
  private fun readGroup(
    id: PwGroupId,
    group: PwGroup
  ) {
    Log.d(
        TAG, "groupId = ${id}, groupName = ${group.name}, " +
        "parent = ${if (group.parent == null) "root" else group.parent.name}"
    )
    for (childGroup in group.childGroups) {
      readGroup(childGroup.id, childGroup)
    }
    for (entry in group.childEntries) {
      readEntry(entry)
    }
  }

  /**
   * 读取条目
   */
  private fun readEntry(entry: PwEntry) {
    Log.d(
        TAG,
        "userName = ${entry.username}, url = ${entry.url}, " +
            "title = ${entry.title}, des = ${entry.notes}, " +
            "parent = ${entry.parent}, password = ${entry.password}"
    )
  }

  /**
   * 测试加密解密
   */
  @Test
  fun encryptStr(){
    val eStr = QuickUnLockUtil.encryptStr("gaJj07uacPAAAAAAAAAANB5oOxdcoY0cwIsjYnwH1yPyws3YUyciD_nfBMoabgG7")
    println("加密字符串：$eStr")
//    val dStr = QuickUnLockUtil.decryption("gaJj07uacPAAAAAAAAAANB5oOxdcoY0cwIsjYnwH1yPyws3YUyciD_nfBMoabgG7")
    val dStr = QuickUnLockUtil.decryption(eStr)
    println("解密字符串：$dStr")
  }

}