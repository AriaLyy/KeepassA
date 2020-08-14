package com.lyy.keepassa.service

import KDBAutoFillRepository
import android.annotation.TargetApi
import android.content.IntentSender
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.preference.PreferenceManager
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.multidatasetservice.AutoFillHelper
import com.lyy.keepassa.service.multidatasetservice.PackageVerifier
import com.lyy.keepassa.service.multidatasetservice.StructureParser
import com.lyy.keepassa.service.multidatasetservice.model.AutoFillFieldMetadataCollection
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.PermissionsUtil
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity

/**
 * 自动填充服务
 * 官方demo https://github.com/android/input-samples
 * 官方文档：https://developer.android.com/reference/android/service/autofill/AutofillService
 */
@TargetApi(Build.VERSION_CODES.O)
class AutoFillService : AutofillService() {
  private val TAG = javaClass.simpleName

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
    if (!PackageVerifier.isValidPackage(applicationContext, apkPackageName)) {
      KLog.e(TAG, "无效的包名：$apkPackageName")
      return
    }
    val data = request.clientState
    KLog.d(TAG, "onFillRequest(): data=" + KLog.b(data))
    cancellationSignal.setOnCancelListener {
      KLog.w(
          TAG, "Cancel autofill not implemented in this sample."
      )
    }

    // Parse AutoFill data in Activity
    val parser = StructureParser(structure)
    parser.parseForFill(isManual)
    val autoFillFields = parser.autoFillFields
    val needAuth = BaseApp.KDB == null || BaseApp.isLocked

    if (autoFillFields.autoFillIds.size <= 0) {
      callback.onSuccess(null)
      return
    }

    // 如果数据库没打开，或者数据库已经锁定，打开登录页面
    if (needAuth) {
      val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
          .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)

      if (BaseApp.KDB == null) {
        openLoginActivity(callback, autoFillFields, apkPackageName)
        return
      }

      if (isOpenQuickLock) {
        openQuickUnLockActivity(callback, autoFillFields, apkPackageName)
        return
      }

      openLoginActivity(callback, autoFillFields, apkPackageName)
      return
    }
    val datas = KDBAutoFillRepository.getFilledAutoFillFieldCollection(apkPackageName)
    // 没有匹配的数据，进入搜索界面
    if (datas == null || datas.isEmpty()) {
      openSearchActivity(callback, autoFillFields, apkPackageName)
      return
    }
    val response =
      AutoFillHelper.newResponse(this, !needAuth, autoFillFields, datas, apkPackageName)
    callback.onSuccess(response)
  }

  /**
   * 启动搜索界面
   */
  private fun openSearchActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String
  ) {
    callback.onSuccess(
        getAuthResponse(
            autofillFields,
            LauncherActivity.getSearchIntentSender(this, apkPackageName)
        )
    )
  }

  /**
   * 启动快速解锁界面
   */
  private fun openQuickUnLockActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String
  ) {
    callback.onSuccess(
        getAuthResponse(
            autofillFields,
            QuickUnlockActivity.getQuickUnlockSenderForResponse(this, apkPackageName)
        )
    )
  }

  /**
   * 启动登录界面
   */
  private fun openLoginActivity(
    callback: FillCallback,
    autofillFields: AutoFillFieldMetadataCollection,
    apkPackageName: String
  ) {
    callback.onSuccess(
        getAuthResponse(
            autofillFields,
            LauncherActivity.getAuthDbIntentSender(this, apkPackageName)
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
//    val responseBuilder = FillResponse.Builder()
//    val presentation = AutoFillHelper
//        .newRemoteViews(
//            this.packageName, getString(R.string.autofill_sign_in_prompt),
//            R.mipmap.ic_launcher
//        )
//    responseBuilder.setAuthentication(metadataList.autoFillIds.toTypedArray(), sender, presentation)

//    return responseBuilder.build()

    return AutoFillHelper.newSaveResponse(this, metadataList, sender)
  }

  /**
   * 保存用户数据
   */
  override fun onSaveRequest(
    request: SaveRequest,
    callback: SaveCallback
  ) {
    val context = request.fillContexts
    val structure = context[context.size - 1].structure
    val apkPackageName = structure.activityComponent.packageName
    if (!PackageVerifier.isValidPackage(applicationContext, apkPackageName)) {
      KLog.e(TAG, "无效的包名：$apkPackageName")
      return
    }
    val data = request.clientState
    KLog.d(TAG, "onSaveRequest(): data=" + KLog.b(data))

    val parser = StructureParser(structure)
    parser.parseForFill(true)
    val needAuth = BaseApp.KDB == null

    // 如果数据库没打开，需要打开登录页面
    if (needAuth) {
      val p = KDBAutoFillRepository.getUserInfo(parser.autoFillFields)
      KLog.d(TAG, "用户信息：$p")
      callback.onSuccess(
          LauncherActivity.getAuthDbIntentSenderBySave(
              context = this,
              apkPackageName = apkPackageName,
              userName = p.first ?: "",
              pass = p.second ?: ""
          )
      )
      return
    }
    if (BaseApp.KDB == null) {
      // 用户没有登陆成功，保存失败
      callback.onFailure(getString(R.string.hint_please_open_database))
      return
    }
    KDBAutoFillRepository.saveDataToKdb(this, apkPackageName, parser.autoFillFields)
    HitUtil.toaskLong(getString(R.string.save_db_success))
    callback.onSuccess()
  }

  override fun onConnected() {
    KLog.d(TAG, "onConnected")
  }

  override fun onDisconnected() {
    KLog.d(TAG, "onDisconnected")
  }

}