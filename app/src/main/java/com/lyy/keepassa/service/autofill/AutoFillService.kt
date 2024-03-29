/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill

import KDBAutoFillRepository
import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.service.autofill.model.AutoFillFieldMetadataCollection
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KdbUtil.isNull
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.util.PermissionsUtil
import com.lyy.keepassa.util.isCanOpenQuickLock
import com.lyy.keepassa.view.create.entry.CreateEntryActivity
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import com.lyy.keepassa.view.search.AutoFillEntrySearchActivity
import timber.log.Timber

/**
 * 自动填充服务
 * 官方demo https://github.com/android/input-samples
 * 官方文档：https://developer.android.com/reference/android/service/autofill/AutofillService
 */
@TargetApi(VERSION_CODES.O)
class AutoFillService : AutofillService() {

  /**
   * 接收请求
   */
  override fun onFillRequest(
    request: FillRequest,
    cancellationSignal: CancellationSignal,
    callback: FillCallback
  ) {
    val isManual = request.flags == FillRequest.FLAG_MANUAL_REQUEST
    val structure = request.fillContexts[request.fillContexts.size - 1].structure
    val apkPackageName = structure.activityComponent.packageName
    if (apkPackageName.equals(packageName, ignoreCase = true)) {
      // 本应用内不进行填充
      return
    }

    if (!PermissionsUtil.isCanBackgroundStart()) {
      ToastUtils.showLong(R.string.hint_open_background_start)
      return
    }

    Timber.d(
      "onFillRequest(): flags = ${request.flags}, requestId = ${request.id}, clientState = ${
        KLog.b(
          request.clientState
        )
      }"
    )
    cancellationSignal.setOnCancelListener {
      Timber.w("Cancel autofill not implemented in this sample.")
    }

    // Parse AutoFill data in Activity
    val parser = StructureParser(structure)
    parser.parseForFill(isManual, apkPackageName)
    val autoFillFields = parser.autoFillFields
    val needAuth = BaseApp.KDB == null || BaseApp.isLocked

    if (autoFillFields.autoFillIds.size <= 0) {
      Timber.i("autoFillIds is nulll")
      callback.onSuccess(null)
      return
    }


    // 如果数据库没打开，或者数据库已经锁定，打开登录页面
    if (needAuth) {
      val isOpenQuickLock = BaseApp.APP.isCanOpenQuickLock()

      if (BaseApp.KDB == null) {
        openLoginActivity(callback, autoFillFields, apkPackageName, structure)
        return
      }

      if (isOpenQuickLock) {
        openQuickUnLockActivity(callback, autoFillFields, apkPackageName, structure)
        return
      }

      openLoginActivity(callback, autoFillFields, apkPackageName, structure)
      return
    }
    // 获取填充数据
    val datas = if (parser.domainUrl.isEmpty()) {
      KDBAutoFillRepository.getAutoFillDataByPackageName(apkPackageName)
    } else {
      KDBAutoFillRepository.getAutoFillDataByDomain(parser.domainUrl)
    }

    Timber.d("entrySize = ${datas?.size}")
    // 没有匹配的数据，进入搜索界面
    if (datas == null) {
      openSearchActivity(callback, autoFillFields, apkPackageName, structure)
      return
    }
    val response =
      AutoFillHelper.newResponse(this, !needAuth, autoFillFields, datas, apkPackageName, structure)
    callback.onSuccess(response)
  }

  /**
   * 启动搜索界面
   */
  private fun openSearchActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String,
    structure: AssistStructure
  ) {
    callback.onSuccess(
      getAuthResponse(
        autofillFields,
        AutoFillEntrySearchActivity.getSearchIntentSender(this, apkPackageName, structure)
      )
    )
  }

  /**
   * 启动快速解锁界面
   */
  private fun openQuickUnLockActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String,
    structure: AssistStructure
  ) {
    callback.onSuccess(
      getAuthResponse(
        autofillFields,
        QuickUnlockActivity.getQuickUnlockSenderForResponse(this, apkPackageName, structure)
      )
    )
  }

  /**
   * 启动登录界面
   */
  private fun openLoginActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String,
    structure: AssistStructure
  ) {
    callback.onSuccess(
      getAuthResponse(
        autofillFields,
        LauncherActivity.getAuthDbIntentSender(this, apkPackageName, structure)
      )
    )
  }

  /**
   * 启动数据库验证界面或数据为空时的匹配界面
   */
  private fun getAuthResponse(
    metadataList: AutoFillFieldMetadataCollection,
    sender: IntentSender
  ): FillResponse {
    return AutoFillHelper.newSaveResponse(this, metadataList, sender)
  }

  /**
   * 保存用户数据
   */
  override fun onSaveRequest(
    request: SaveRequest,
    callback: SaveCallback
  ) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.P) {
      val str = ResUtil.getString(R.string.fail_unsupported_Systems_O)
      callback.onFailure(str)
      ToastUtils.showLong(str)
      return
    }
    val context = request.fillContexts
    val structure = context[context.size - 1].structure
    val apkPackageName = structure.activityComponent.packageName
    val data = request.clientState
    Timber.d("onSaveRequest(): data=${KLog.b(data)}")

    val parser = StructureParser(structure)
    parser.parseForFill(true, apkPackageName)
    val needAuth = BaseApp.KDB.isNull() || BaseApp.isLocked

    // 如果数据库没打开，需要打开登录页面
    val p = KDBAutoFillRepository.getUserInfo(parser.autoFillFields)
    Timber.d("用户信息：$p")
    if (needAuth) {
      // This api is only at P
      callback.onSuccess(
        LauncherActivity.authAndSaveDb(
          context = this,
          apkPackageName = apkPackageName,
          userName = p.first ?: "",
          pass = p.second ?: "",
          if (!BaseApp.KDB.isNull() && BaseApp.APP.isCanOpenQuickLock()) QuickUnlockActivity::class.java else LauncherActivity::class.java
        )
      )
      return
    }
    if (BaseApp.KDB == null) {
      // 用户没有登陆成功，保存失败
      callback.onFailure(getString(R.string.hint_please_open_database))
      return
    }

    // KDBAutoFillRepository.saveDataToKdb(this, apkPackageName, parser.autoFillFields)
    callback.onSuccess(
      CreateEntryActivity.authAndSaveDb(
        this, AutoFillParam(
          apkPkgName = apkPackageName,
          saveUserName = p.first ?: "",
          savePass = p.second ?: "",
          isSave = true
        )
      )
    )
    HitUtil.toaskLong(getString(R.string.save_db_success))
  }

  override fun onConnected() {
    Timber.d("onConnected")
  }

  override fun onDisconnected() {
    Timber.d("onDisconnected")
    // W3cHints.curDomainUrl = ""
  }

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(LanguageUtil.setLanguage(newBase!!, BaseApp.currentLang))
  }
}