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
package com.lyy.keepassa.service.multidatasetservice

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.service.multidatasetservice.model.AutoFillFieldMetadataCollection
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.tencent.bugly.crashreport.BuglyLog

/**
 * This is a class containing helper methods for building Autofill Datasets and Responses.
 */
@TargetApi(Build.VERSION_CODES.O)
object AutoFillHelper {
  val TAG = javaClass.simpleName
  const val EXTRA_DATASET_NAME = "dataset_name"
  const val EXTRA_FOR_RESPONSE = "for_response"

  val CompatBrowsers = setOf(
      "org.mozilla.firefox",
      "org.mozilla.firefox_beta",
      "com.microsoft.emmx",
      "com.android.chrome",
      "com.chrome.beta",
      "com.android.browser",
      "com.brave.browser",
      "com.opera.browser",
      "com.opera.browser.beta",
      "com.opera.mini.native",
      "com.chrome.dev",
      "com.chrome.canary",
      "com.google.android.apps.chrome",
      "com.google.android.apps.chrome_dev",
      "com.yandex.browser",
      "com.sec.android.app.sbrowser",
      "com.sec.android.app.sbrowser.beta",
      "org.codeaurora.swe.browser",
      "com.amazon.cloud9",
      "mark.via.gp",
      "org.bromite.bromite",
      "org.chromium.chrome",
      "com.kiwibrowser.browser",
      "com.ecosia.android",
      "com.opera.mini.native.beta",
      "org.mozilla.fennec_aurora",
      "org.mozilla.fennec_fdroid",
      "com.qwant.liberty",
      "com.opera.touch",
      "org.mozilla.fenix",
      "org.mozilla.fenix.nightly",
      "org.mozilla.reference.browser",
      "org.mozilla.rocket",
      "org.torproject.torbrowser",
      "com.vivaldi.browser"
  )

  /**
   * 数据库没打开时的view
   * @param packageName 当前应用的包名（keepassA的包名)
   */
  fun newRemoteViews(
    packageName: String,
    remoteViewsText: String,
    @DrawableRes drawableId: Int
  ): RemoteViews {
    val presentation = RemoteViews(packageName, R.layout.item_auto_fill)
    presentation.setTextViewText(R.id.text, remoteViewsText)
    presentation.setImageViewResource(R.id.img, drawableId)
    return presentation
  }

  /**
   * 创建填充数据，填充用户名，密码
   * @param dataSetAuth true 验证通过
   * @param apkPageName 第三方apk包名
   */
  fun newDataSet(
    context: Context,
    metadataList: AutoFillFieldMetadataCollection,
    entry: PwEntry?,
    dataSetAuth: Boolean,
    apkPageName: String
  ): Dataset? {
    if (entry == null) {
      return null
    }

    val dataSetBuilder: Dataset.Builder
    if (!dataSetAuth) {
      dataSetBuilder = Dataset.Builder(
          buildRemoteView(
              context.packageName, entry.title,
              if (entry is PwEntryV4) entry.customIcon else null,
              entry.icon
          )
      )
      // 设置点击事件，用于数据库没有打开的情况
      val sender = LauncherActivity.getAuthDbIntentSender(context, apkPageName)
      dataSetBuilder.setAuthentication(sender)
    } else {
      dataSetBuilder = Dataset.Builder(
          buildRemoteView(
              context.packageName, entry.title,
              if (entry is PwEntryV4) entry.customIcon else null,
              entry.icon
          )
      )
    }
    // 填充数据
    val setValueAtLeastOnce = applyToFields(entry, metadataList, dataSetBuilder)
    if (setValueAtLeastOnce) {
      return dataSetBuilder.build()
    }
    return null
  }

  /**
   * 构建用户名view
   */
  private fun buildRemoteView(
    pkgName: String,
    title: String,
    customIcon: PwIconCustom?,
    icon: PwIconStandard
  ): RemoteViews {
    val rev = RemoteViews(pkgName, R.layout.item_auto_fill)
    rev.setTextViewText(R.id.text, title)
    if (customIcon != null && customIcon.imageData.isNotEmpty()) {
      val byte = customIcon.imageData
      val option = BitmapFactory.Options()
      option.inPreferredConfig = Config.RGB_565
      option.inDensity = 480
      rev.setImageViewBitmap(R.id.img, BitmapFactory.decodeByteArray(byte, 0, byte.size, option))
    } else {
      rev.setImageViewResource(R.id.img, IconUtil.getIconById(icon.iconId))
    }
    return rev
  }

  /**
   * 返回查找不到条目的响应，不返回SaveInfo，系统是不会触发保存的
   */
  fun newSaveResponse(
    context: Context,
    metadataList: AutoFillFieldMetadataCollection,
    sender: IntentSender
  ): FillResponse {
    val responseBuilder = FillResponse.Builder()
    val presentation = newRemoteViews(
        context.packageName, context.getString(R.string.autofill_sign_in_prompt),
        R.mipmap.ic_launcher
    )
    responseBuilder.setAuthentication(metadataList.autoFillIds.toTypedArray(), sender, presentation)
    val datasetBuild = Dataset.Builder()
    val notUsed = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
    val b = applyToFields(metadataList, datasetBuild, notUsed)
    KLog.d(TAG, "applyToFields -> $b")
    if (b){
      responseBuilder.addDataset(datasetBuild.build())
    }

    /*
     * 触发系统保存弹窗的条件：
     * 1、activity 必须要关闭
     * 2、editText 中的值必须有更改。如果editText中已经设置了text，这时直接登陆的话，是不会触发弹框的
     * 3、必须设置DataSet
     * 4、也可以手动触发，不需要等待activity关闭，但是需要设置setTriggerId
     */
    val sbi = SaveInfo.Builder(metadataList.saveType, metadataList.autoFillIds.toTypedArray())
//        .setOptionalIds(metadataList.autoFillIds.toTypedArray())
        .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        .build()

    responseBuilder.setSaveInfo(sbi)

    return responseBuilder.build()
  }

  /**
   * @param dataSetAuth true 验证通过
   * @param apkPageName 第三方apk包名
   */
  fun newResponse(
    context: Context,
    dataSetAuth: Boolean,
    metadata: AutoFillFieldMetadataCollection,
    entries: MutableList<PwEntry>?,
    apkPageName: String
  ): FillResponse? {
    val responseBuilder = FillResponse.Builder()

    entries?.forEach { entry ->
      val dataset = newDataSet(context, metadata, entry, dataSetAuth, apkPageName)
      dataset?.let(responseBuilder::addDataset)
    }

    return if (metadata.saveType != 0) {
      val autoFillIds = metadata.autoFillIds
      // 设置触发保存的类型
      responseBuilder.setSaveInfo(
          SaveInfo.Builder(
              metadata.saveType,
              autoFillIds.toTypedArray()
          )
              .build()
      )
      responseBuilder.build()
    } else {
      KLog.d(TAG, "These fields are not meant to be saved by autofill.")
      null
    }
  }

  fun isValidHint(hint: String): Boolean {
    when (hint) {
      View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
      View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY,
      View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
      View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
      View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
      View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
      View.AUTOFILL_HINT_EMAIL_ADDRESS,
      View.AUTOFILL_HINT_PHONE,
      View.AUTOFILL_HINT_NAME,
      View.AUTOFILL_HINT_PASSWORD,
      View.AUTOFILL_HINT_POSTAL_ADDRESS,
      View.AUTOFILL_HINT_POSTAL_CODE,
      View.AUTOFILL_HINT_USERNAME ->
        return true
      else ->
        return false
    }
  }

  /**
   * 将数据填充到FillResponse中
   */
  private fun applyToFields(
    pwEntry: PwEntry,
    autoFillFieldMetadataList: AutoFillFieldMetadataCollection,
    dataSetBuilder: Dataset.Builder
  ): Boolean {
    var setValueAtLeastOnce = false
    for (hint in autoFillFieldMetadataList.allAutoFillHints) {
      val fillFields = autoFillFieldMetadataList.getFieldsForHint(hint) ?: continue
      loop@ for (fillField in fillFields) {
        val fillId = fillField.autoFillId
        if (fillId == null) {
          BuglyLog.e(TAG, "autofill == null")
          break
        }
        val fillType = fillField.autoFillType
        KLog.w(TAG, "autofill type -> $fillType, autofillId -> $fillId")
        when (fillType) {
          View.AUTOFILL_TYPE_LIST -> {
            if (fillField.autoFillField.textValue.isNullOrEmpty()) {
              continue@loop
            }

            val listValue = fillField.getAutoFillOptionIndex(fillField.autoFillField.textValue!!)
            if (listValue != 1) {
              dataSetBuilder.setValue(fillId, AutofillValue.forList(listValue))
              setValueAtLeastOnce = true
            }
          }

          View.AUTOFILL_TYPE_DATE -> {
            fillField.autoFillField.dateValue ?: continue@loop

            dataSetBuilder.setValue(
                fillId,
                AutofillValue.forDate(fillField.autoFillField.dateValue!!)
            )
            setValueAtLeastOnce = true
          }

          View.AUTOFILL_TYPE_TEXT -> {
            if (fillField.isPassword) {
              dataSetBuilder.setValue(fillId, AutofillValue.forText(pwEntry.password))
            } else {
              dataSetBuilder.setValue(fillId, AutofillValue.forText(pwEntry.username))
            }
            setValueAtLeastOnce = true
          }

          View.AUTOFILL_TYPE_TOGGLE -> {
            fillField.autoFillField.toggleValue ?: continue@loop

            dataSetBuilder.setValue(
                fillId,
                AutofillValue.forToggle(fillField.autoFillField.toggleValue!!)
            )
            setValueAtLeastOnce = true
          }
          else -> KLog.w(TAG, "Invalid autofill type -> $fillType")
        }
      }
    }
    return setValueAtLeastOnce
  }

  /**
   * 将数据填充到FillResponse中
   */
  private fun applyToFields(
    autoFillFieldMetadataList: AutoFillFieldMetadataCollection,
    dataSetBuilder: Dataset.Builder,
    rv: RemoteViews
  ): Boolean {
    var setValueAtLeastOnce = false
    for (hint in autoFillFieldMetadataList.allAutoFillHints) {
      val fillFields = autoFillFieldMetadataList.getFieldsForHint(hint) ?: continue
      loop@ for (fillField in fillFields) {
        val fillId = fillField.autoFillId
        if (fillId == null) {
          BuglyLog.e(TAG, "autofill == null")
          break
        }
        val fillType = fillField.autoFillType
        KLog.w(TAG, "autofill type -> $fillType, autofillId -> $fillId")
        when (fillType) {
          View.AUTOFILL_TYPE_LIST -> {
            if (fillField.autoFillField.textValue.isNullOrEmpty()) {
              continue@loop
            }

            val listValue = fillField.getAutoFillOptionIndex(fillField.autoFillField.textValue!!)
            if (listValue != 1) {
              dataSetBuilder.setValue(fillId, AutofillValue.forList(listValue))
              setValueAtLeastOnce = true
            }
          }

          View.AUTOFILL_TYPE_DATE -> {
            fillField.autoFillField.dateValue ?: continue@loop

            dataSetBuilder.setValue(
                fillId,
                AutofillValue.forDate(fillField.autoFillField.dateValue!!)
            )
            setValueAtLeastOnce = true
          }

          View.AUTOFILL_TYPE_TEXT -> {
            if (fillField.autoFillField.textValue.isNullOrEmpty()) {
              continue@loop
            }
            dataSetBuilder.setValue(
                fillId,
                AutofillValue.forText(fillField.autoFillField.textValue),
                rv
            )
            setValueAtLeastOnce = true
          }

          View.AUTOFILL_TYPE_TOGGLE -> {
            fillField.autoFillField.toggleValue ?: continue@loop

            dataSetBuilder.setValue(
                fillId,
                AutofillValue.forToggle(fillField.autoFillField.toggleValue!!)
            )
            setValueAtLeastOnce = true
          }
          else -> KLog.w(TAG, "Invalid autofill type -> $fillType")
        }
      }
    }
    return setValueAtLeastOnce
  }
}