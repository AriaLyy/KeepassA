/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.util.Log
import android.view.View
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV3
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.multidatasetservice.model.AutoFillFieldMetadataCollection
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Singleton autofill data repository that stores autofill fields to SharedPreferences.
 * Disclaimer: you should not store sensitive fields like user data unencrypted. This is done
 * here only for simplicity and learning purposes.
 */
object KDBAutoFillRepository {
  private val TAG = "KDBAutoFillRepository"

  /**
   * 从KDB数据库获取数据
   */
  fun getFilledAutoFillFieldCollection(pkgName: String): ArrayList<PwEntry>? {
    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchAutoFillEntries(pkgName, listStorage)
    if (listStorage.isEmpty()) {
      return null
    }

    return listStorage
  }

  /**
   * 保存数据到数据库
   */
  fun saveDataToKdb(
    context: Context,
    apkPkgName: String,
    autofillFields: AutoFillFieldMetadataCollection
  ) {

    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchAutoFillEntries(apkPkgName, listStorage)
    val entry: PwEntry
    if (listStorage.isEmpty()) {
      if (BaseApp.isV4) {
        entry = PwEntryV4(BaseApp.KDB.pm.rootGroup as PwGroupV4)
        val icon = getAppIcon(context, apkPkgName)
        if (icon != null) {
          val baos = ByteArrayOutputStream()
          icon.compress(PNG, 100, baos)
          val datas: ByteArray = baos.toByteArray()
          val customIcon = PwIconCustom(UUID.randomUUID(), datas)
          entry.customIcon = customIcon
          (BaseApp.KDB.pm as PwDatabaseV4).putCustomIcons(customIcon)
          entry.strings["KP2A_URL_1"] = ProtectedString(false, "androidapp://$apkPkgName")
        }
      } else {
        entry = PwEntryV3()
        entry.setUrl("androidapp://$apkPkgName", BaseApp.KDB.pm)
      }
      val appName = getAppName(context, apkPkgName)
      entry.setTitle(appName ?: "newEntry", BaseApp.KDB.pm)
      entry.icon = PwIconStandard(0)
      GlobalScope.launch {
        KdbUtil.addEntry(entry, save = false)
      }
    } else {
      entry = listStorage[0]
      Log.w(TAG, "已存在含有【$apkPkgName】的条目，将更新条目")
    }

    for (hint in autofillFields.allAutoFillHints) {
      val fillFields = autofillFields.getFieldsForHint(hint) ?: continue
      for (fillField in fillFields) {
        fillField.autoFillField.textValue ?: continue
        if (fillField.autoFillType == View.AUTOFILL_TYPE_TEXT) {
          if (fillField.isPassword) {
            entry.setPassword(fillField.autoFillField.textValue, BaseApp.KDB.pm)
//            Log.d(TAG, "pass = ${fillField.textValue}")
          } else {
            entry.setUsername(fillField.autoFillField.textValue, BaseApp.KDB.pm)
//            Log.d(TAG, "userName = ${fillField.textValue}")
          }
        }
      }
    }
    GlobalScope.launch {
      KdbUtil.saveDb(uploadDb = false)
    }
    Log.d(TAG, "密码信息保存成功")
  }

  /**
   * 获取用户名和密码
   * @return first 用户名
   */
  fun getUserInfo(autofillFields: AutoFillFieldMetadataCollection): Pair<String?, String?> {
    var user: String? = null
    var pass: String? = null
    for (hint in autofillFields.allAutoFillHints) {
      val fillFields = autofillFields.getFieldsForHint(hint) ?: continue
      for (fillField in fillFields) {
        fillField.autoFillField.textValue ?: continue
        if (fillField.autoFillType == View.AUTOFILL_TYPE_TEXT) {
          if (fillField.isPassword && pass == null) {
            pass = fillField.autoFillField.textValue
          }
          if (!fillField.isPassword && user == null) {
            user = fillField.autoFillField.textValue
          }
        }
      }
    }
    return Pair(user, pass)
  }

  /**
   * 获取应用程序名称
   */
  fun getAppName(
    context: Context,
    apkPkgName: String
  ): String? {
    try {
      val packageManager = context.packageManager
      return packageManager.getApplicationLabel(
          packageManager.getApplicationInfo(
              apkPkgName,
              PackageManager.GET_META_DATA
          )
      ).toString()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  /**
   * 获取图标 bitmap
   * @param context
   */
  fun getAppIcon(
    context: Context,
    apkPkgName: String
  ): Bitmap? {
    val d = context.packageManager.getApplicationIcon(apkPkgName) ?: return null
    return IconUtil.getBitmapFromDrawable(context, d)
  }

}