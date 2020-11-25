/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.detail

import android.content.Context
import android.net.Uri
import android.text.InputType
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.liveData
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.Types
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.view.menu.EntryDetailFilePopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu
import com.lyy.keepassa.view.menu.EntryDetailStrPopMenu.OnShowPassCallback
import com.lyy.keepassa.widget.expand.AttrFileItemView
import com.lyy.keepassa.widget.expand.AttrStrItemView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 条目详情
 */
class EntryDetailModule : BaseModule() {
  var curDLoadFile: ProtectedBinary? = null
  val createFileRequestCode = 0xA1

  /**
   * 展示附件的菜单
   */
  fun showAttrFilePopMenu(
    context: FragmentActivity,
    v: View
  ) {
    if (KeepassAUtil.isFastClick()) {
      return
    }
    val key = (v as AttrFileItemView).titleStr
    val value = v.file
    val menu = EntryDetailFilePopMenu(context, v, key, value!!)
    menu.setOnDownloadClick(object : EntryDetailFilePopMenu.OnDownloadClick {
      override fun onDownload(
        key: String,
        file: ProtectedBinary
      ) {
        curDLoadFile = file
        KeepassAUtil.createFile(context, "*/*", key, createFileRequestCode)
      }
    })
    menu.show()
  }

  /**
   * 展示属性字段的菜单
   */
  fun showAttrStrPopMenu(
    context: FragmentActivity,
    v: View
  ) {
    if (KeepassAUtil.isFastClick()) {
      return
    }
    val value = v.findViewById<TextView>(R.id.value)
    val key = (v as AttrStrItemView).titleStr
    val str = v.valueInfo
    val pop = EntryDetailStrPopMenu(context, v, str)
    // totp 密码，seed都需要显示密码
    if (key == "TOTP"
        || key.equals("otp", ignoreCase = true)
        || key.equals("TOTP Seed", ignoreCase = true)
        || str.isProtected
    ) {
      pop.setOnShowPassCallback(object : OnShowPassCallback {
        override fun showPass(showPass: Boolean) {
          if (showPass) {
            value.inputType =
              (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            return
          }
          value.inputType =
            (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        }
      })

      if (value.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
        pop.setHidePass()
      }
    }

    pop.show()
  }

  /**
   * 保存附件到sd卡
   * @param saveUri 保存路径
   * @param source 需要保存的文件
   */
  fun saveAttachment(
    context: Context,
    saveUri: Uri,
    source: ProtectedBinary
  ) = liveData {
    val fileName = withContext(Dispatchers.IO) {
      try {
        val byte = source.data.readBytes()
        val os = context.contentResolver.openOutputStream(saveUri)
        if (os != null) {
          os.write(byte, 0, byte.size)
          os.flush()
          os.close()
        }
        return@withContext UriUtil.getFileNameFromUri(context, saveUri)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext null
    }
    emit(fileName)
  }

  /**
   * 回收项目
   * @param pwEntry 需要回收的条目
   */
  fun recycleEntry(pwEntry: PwEntry) = liveData {
    val code = withContext(Dispatchers.IO) {
      try {
        if (BaseApp.isV4) {
          val v4Entry = pwEntry as PwEntryV4
          if (BaseApp.KDB.pm.canRecycle(v4Entry)) {
            BaseApp.KDB.pm.recycle(v4Entry)
          } else {
            KdbUtil.deleteEntry(pwEntry, false, needUpload = false)
          }
        } else {
          KdbUtil.deleteEntry(pwEntry, false, needUpload = false)
        }
        // 保存数据
        return@withContext KdbUtil.saveDb()
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext DbSynUtil.STATE_DEL_FILE_FAIL
    }
    emit(code)
  }

  /**
   * 保存打开记录
   */
  fun saveRecord(pwEntry: PwEntry) {
    GlobalScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.entryRecordDao()
      var record = dao.getRecord(Types.UUIDtoBytes(pwEntry.uuid), BaseApp.dbRecord.localDbUri)
      if (record == null) {
        record = EntryRecord(
            userName = pwEntry.username,
            title = pwEntry.title,
            uuid = Types.UUIDtoBytes(pwEntry.uuid),
            time = System.currentTimeMillis(),
            dbFileUri = BaseApp.dbRecord.localDbUri
        )
        dao.saveRecord(record)
      } else {
        record.title = pwEntry.title
        record.userName = pwEntry.username
        record.time = System.currentTimeMillis()
        dao.updateRecord(record)
      }
      EventBus.getDefault()
          .post(record)
    }
  }

  /**
   * 获取项目的属性字段，只有v4版本才有自定义属性字段
   */
  fun getV4EntryStr(entryV4: PwEntryV4): Map<String, ProtectedString> {
    return KeepassAUtil.filterCustomStr(entryV4)
  }

}