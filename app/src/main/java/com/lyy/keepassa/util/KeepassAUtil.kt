/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import KDBAutoFillRepository
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.assist.AssistStructure
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.core.AbsFrame
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.StringUtil
import com.blankj.utilcode.util.AppUtils
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.router.ActivityRouter
import com.lyy.keepassa.router.ServiceRouter
import com.lyy.keepassa.service.autofill.AutoFillHelper
import com.lyy.keepassa.service.autofill.StructureParser
import com.lyy.keepassa.view.create.CreateDbActivity
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.launcher.OpenDbHistoryActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import com.lyy.keepassa.view.search.AutoFillEntrySearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar

class KeepassAUtil private constructor() {
  companion object {
    val instance: KeepassAUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      KeepassAUtil()
    }
  }

  private var LAST_CLICK_TIME = System.currentTimeMillis()

  fun getAppVersionCode(context: Context): Int {
    val manager = context.packageManager
    var code = 0
    try {
      val info = manager.getPackageInfo(context.packageName, 0)
      code = info.versionCode
    } catch (e: NameNotFoundException) {
      Timber.e(e)
    }
    return code
  }

  fun getAppVersionName(context: Context): String {
    val manager = context.packageManager
    var name: String? = null
    try {
      val info = manager.getPackageInfo(context.packageName, 0)
      name = info.versionName
    } catch (e: NameNotFoundException) {
      Timber.e(e)
    }
    return name!!
  }

  /**
   * 获取进程号对应的进程名
   *
   * @param pid 进程号
   * @return 进程名
   */
  fun getProcessName(pid: Int): String? {
    var reader: BufferedReader? = null
    try {
      reader = BufferedReader(FileReader("/proc/$pid/cmdline"))
      var processName: String = reader.readLine()
      if (!TextUtils.isEmpty(processName)) {
        processName = processName.trim { it <= ' ' }
      }
      return processName
    } catch (throwable: Throwable) {
      Timber.e(throwable)
    } finally {
      try {
        reader?.close()
      } catch (exception: IOException) {
        Timber.e(exception)
      }
    }
    return null
  }

  /**
   * is auto lock the database
   * @return true auto lock the database
   */
  fun isAutoLockDb(): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
      .getBoolean(BaseApp.APP.getString(R.string.set_key_auto_lock_database), true)
  }

  /**
   * is display loading anim
   * @return true display loading anim
   */
  fun isDisplayLoadingAnim(): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
      .getBoolean(BaseApp.APP.getString(R.string.set_key_loading_anim), true)
  }

  /**
   * start lock timer
   * @param obj fragment or activity
   */
  fun startLockTimer(obj: Any?) {
    KpaUtil.scope.launch(Dispatchers.IO) {
      // delay 1s, in order to wait for the configuration file to take effect
      delay(1000)
      if (!isAutoLockDb()) {
        return@launch
      }
      if (isNeedStartLockActivity(obj)) {
        if (BaseApp.isLocked) {
          AutoLockDbUtil.get()
            .startLockWorkerNow()
          return@launch
        }

        if (AppUtils.isAppForeground()) {
          AutoLockDbUtil.get().resetTimer()
          return@launch
        }
      }
    }
  }

  /**
   * lock the db
   */
  fun lock() {
    Timber.d("锁定数据库")
    BaseApp.isLocked = true
    val isOpenQuickLock = BaseApp.APP.isCanOpenQuickLock()
    // 只有应用在前台才会跳转到锁屏页面
    if (AppUtils.isAppForeground() && BaseApp.KDB != null) {
      // 开启快速解锁则跳转到快速解锁页面
      if (isOpenQuickLock) {
        NotificationUtil.startQuickUnlockNotify(BaseApp.APP)
        val cActivity = AbsFrame.getInstance().currentActivity
//        if (cActivity != null && cActivity is QuickUnlockActivity) {
//          Timber.w("快速解锁已启动，不再启动快速解锁")
//          return
//        }

        Timber.d("启动快速解锁")
        BaseApp.APP.startActivity(Intent(Intent.ACTION_MAIN).also {
          it.component =
            ComponentName(
              BaseApp.APP.packageName,
              "${BaseApp.APP.packageName}.view.main.QuickUnlockActivity"
            )
          it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return
      }

      // 没有开启快速解锁，则回到启动页
      NotificationUtil.startDbLocked(BaseApp.APP)
      val cActivity = AbsFrame.getInstance().currentActivity
      if (cActivity != null && cActivity is LauncherActivity) {
        Timber.w("解锁页面已启动，不再启动快速解锁")
        return
      }
      Timber.d("快速解锁没有启动，进入解锁界面")
      Routerfit.create(ServiceRouter::class.java).getDbSaveService().clearDb()
      BaseApp.APP.startActivity(Intent(Intent.ACTION_MAIN).also {
        it.component =
          ComponentName(
            BaseApp.APP.packageName,
            "${BaseApp.APP.packageName}.view.launcher.LauncherActivity"
          )
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      })
      for (ac in AbsFrame.getInstance().activityStack) {
        if (isHomeActivity(ac)) {
          continue
        }
        ac.finish()
      }
      return
    }

    // 处理处于后台的情况
    if (isOpenQuickLock) {
      NotificationUtil.startQuickUnlockNotify(BaseApp.APP)
      return
    }
    Routerfit.create(ServiceRouter::class.java).getDbSaveService().clearDb()
    NotificationUtil.startDbLocked(BaseApp.APP)
  }

  fun isHomeActivity(ac: Activity): Boolean {
    val clazz = ac.javaClass
    return (clazz == LauncherActivity::class.java
      || clazz == CreateDbActivity::class.java
      || clazz == OpenDbHistoryActivity::class.java
      || clazz == QuickUnlockActivity::class.java
      )
  }

  /**
   * 判断颜色是不是亮色
   *
   * @param color
   * @return
   * @from https://stackoverflow.com/questions/24260853/check-if-color-is-dark-or-light-in-android
   */
  fun isLightColor(@ColorInt color: Int): Boolean {
    return ColorUtils.calculateLuminance(color) >= 0.5
  }

  /**
   * is night mode
   * @return true yes, false no
   */
  fun isNightMode(): Boolean {
    return BaseApp.APP.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
  }

  /**
   * 是否需要启动快速解锁
   * @return true 启动快速解锁
   */
  private fun isNeedStartLockActivity(obj: Any?): Boolean {
    if (obj == null) {
      return false
    }
    if (obj is Fragment && obj.activity == null) {
      return false
    }

    val clazz = if (obj is Fragment) {
      obj.requireActivity().javaClass
    } else {
      obj.javaClass
    }

    return (
      clazz != LauncherActivity::class.java
        && clazz != QuickUnlockActivity::class.java
        && clazz != CreateDbActivity::class.java
        && clazz != AutoFillEntrySearchActivity::class.java
        && clazz != OpenDbHistoryActivity::class.java
      )
  }

  /**
   * 重新打开数据库
   * 如果数据库没有打开或者没有启动快速解锁 跳转到数据库打开页面
   * 否则跳转到快速启动页
   */
  fun reOpenDb(context: Context) {
    if (!BaseApp.APP.isCanOpenQuickLock()) {
      ARouter.getInstance()
        .build("/launcher/activity")
        .withInt(LauncherActivity.KEY_OPEN_TYPE, LauncherActivity.OPEN_TYPE_OPEN_DB)
        .navigation()
      for (ac in AbsFrame.getInstance().activityStack) {
        if (ac is LauncherActivity) {
          continue
        }
        ac.finish()
      }
      return
    }
    QuickUnlockActivity.startQuickUnlockActivity(context)
  }

  /**
   * 回到启动页
   * @param turnFragment [LauncherActivity.OPEN_TYPE_CHANGE_DB]选择数据库页面，
   * [LauncherActivity.OPEN_TYPE_OPEN_DB] 打开数据库
   */
  fun turnLauncher(turnFragment: Int = LauncherActivity.OPEN_TYPE_CHANGE_DB) {
    BaseApp.isLocked = true
    ARouter.getInstance()
      .build("/launcher/activity")
      .withInt(LauncherActivity.KEY_OPEN_TYPE, turnFragment)
      .navigation()
    for (ac in AbsFrame.getInstance().activityStack) {
      if (ac is LauncherActivity) {
        continue
      }
      ac.finish()
    }
  }

  /**
   * 检查http地址是否有效
   * @return false 无效，true 有效
   */
  fun checkUrlIsValid(url: String?): Boolean {
    if (url.isNullOrBlank()) {
      return false
    }
    val rs = "^(http|https)://[^\\s]*"
//      "^(((file|gopher|news|nntp|telnet|http|ftp|https|ftps|sftp)://)|(www\\.))+(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(/[a-zA-Z0-9\\&%_\\./-~-]*)?\$"
    val r1 = Regex(rs, RegexOption.IGNORE_CASE)
    return r1.matches(url)
  }

  /**
   * 将pwentry 转换为列表实体
   */
  fun convertPwEntry2Item(entry: PwEntry): SimpleItemEntity {
    val item = SimpleItemEntity()
    item.title = entry.title
    item.subTitle = if (entry.isRef()) {
      val refStr = "${BaseApp.APP.resources.getString(R.string.ref_entry)}: "
      val tempStr = "${refStr}${KdbUtil.getUserName(entry)}"
      StringUtil.highLightStr(tempStr, refStr, ResUtil.getColor(R.color.colorPrimary), true)
    } else {
      entry.username
    }
    item.obj = entry
    return item
  }

  /**
   * 将pwGroup 转换为列表实体
   */
  fun convertPwGroup2Item(pwGroup: PwGroup): SimpleItemEntity {
    val item = SimpleItemEntity()
    item.title = pwGroup.name
    item.subTitle = ResUtil.getString(
      R.string.hint_group_desc, KdbUtil.getGroupEntryNum(pwGroup)
        .toString()
    )
    item.obj = pwGroup
    return item
  }

  /**
   * 保存上一次打开的数据库记录
   */
  fun saveLastOpenDbHistory(record: DbHistoryRecord?) {
    if (record == null) {
      return
    }
    KpaUtil.scope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.dbRecordDao()
      val his = dao.findRecord(record.localDbUri)
      if (his == null || his.localDbUri.isEmpty()) {
        record.uid = 0 // 保证uid能自增且不冲突
        record.time = System.currentTimeMillis()
        dao.saveRecord(record)
        BaseApp.dbRecord = record
        Timber.d("保存数据库打开记录成功")
        return@launch
      }

      his.keyUri = record.keyUri
      his.cloudDiskPath = record.cloudDiskPath
      his.type = record.type
      his.time = record.time
      dao.updateRecord(his)
      BaseApp.dbRecord = his
      Timber.d("更新数据库打开记录成功")
    }
  }

  /**
   *  自动填充的response
   *  @param intent 自动填充服务床进来的intentc
   *  @param apkPkgName 第三方包名
   */
  @TargetApi(Build.VERSION_CODES.O)
  fun getFillResponse(
    context: Context,
    intent: Intent,
    apkPkgName: String
  ): Intent {
    val autoFillStructure = intent.getParcelableExtra<AssistStructure>(
      AutofillManager.EXTRA_ASSIST_STRUCTURE
    )
    if (autoFillStructure == null) {
      Timber.e("autoFillStructure is null")
      return Intent()
    }

    val parser = StructureParser(autoFillStructure)
    parser.parseForFill(true, apkPkgName)
    val autofillFields = parser.autoFillFields

    val datas = KDBAutoFillRepository.getAutoFillDataByPackageName(apkPkgName)
    val response =
      AutoFillHelper.newResponse(
        context,
        true,
        autofillFields,
        datas,
        apkPkgName,
        autoFillStructure
      )

    val data = Intent()
    data.putExtra(LauncherActivity.KEY_PKG_NAME, apkPkgName)
    data.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
    return data
  }

  /**
   *  自动填充的response
   *  @param intent 自动填充服务床进来的intent
   *  @param pwEntry u数据条目
   */
  @TargetApi(Build.VERSION_CODES.O)
  fun getFillResponse(
    context: Context,
    intent: Intent,
    pwEntry: PwEntry,
    apkPkgName: String
  ): Intent {
    val autoFillStructure = intent.getParcelableExtra<AssistStructure>(
      AutofillManager.EXTRA_ASSIST_STRUCTURE
    )
    if (autoFillStructure == null) {
      Timber.e("autoFillStructure is null")
      return Intent()
    }
    val parser = StructureParser(autoFillStructure)
    parser.parseForFill(true, apkPkgName)
    val autofillFields = parser.autoFillFields

    val response =
      AutoFillHelper.newResponse(
        context,
        true,
        autofillFields,
        arrayListOf(pwEntry),
        apkPkgName,
        autoFillStructure
      )

    val data = Intent()
    data.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
    return data
  }

  /**
   * 转换uri
   */
  fun convertUri(uriString: String?): Uri? {
    if (uriString == null) {
      return null
    }
    if (uriString.equals("null", ignoreCase = true)) {
      return null
    }
    val temp = Uri.parse(uriString)
//    val ub = Uri.Builder()
//    ub.authority("com.android.externalstorage.documents") // 必须设置，否则8.0上会出现崩溃的问题
//    val uri = ub.scheme(temp.scheme)
//        .path(temp.path)
//        .appendPath(temp.path)
//        .encodedPath(temp.encodedPath)
//        .fragment(temp.fragment)
//        .encodedFragment(temp.encodedFragment)
//        .query(temp.query)
//        .encodedQuery(temp.encodedQuery)
//        .build()
//    Log.d(TAG, "new uri = $uri")
//    Log.d(TAG, "temp = $temp")
//    Log.d(TAG, "uriString = $uriString")
    return temp
  }

  fun isFastClick(): Boolean {
    val isFast = System.currentTimeMillis() - LAST_CLICK_TIME < 400
    LAST_CLICK_TIME = System.currentTimeMillis()
    return isFast
  }

  /**
   * 截取并保存短密码
   */
  fun subShortPass() {
    val sh = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
    val subType = sh.getString(BaseApp.APP.getString(R.string.set_quick_pass_type), "1")!!
      .toString()
      .toInt()
    val passLen = sh.getString(BaseApp.APP.getString(R.string.set_quick_pass_len), "3")!!
      .toString()
      .toInt()
    val masterPass = QuickUnLockUtil.decryption(BaseApp.dbPass)
    var shortPass = ""
    Timber.i("截取短密码，长度：$passLen，截取类型：$subType")
    when (subType) {
      // 前面位
      1 -> {
        shortPass =
          if (masterPass.length <= passLen) masterPass else masterPass.substring(0, passLen)
      }
      // 末尾
      2 -> {
        shortPass =
          if (masterPass.length <= passLen) masterPass else masterPass.substring(
            masterPass.length - passLen, masterPass.length
          )
      }
    }
    Timber.d("shortPass = $shortPass")
    BaseApp.shortPass = QuickUnLockUtil.encryptStr(shortPass)
  }

  /**
   * 过滤并排序自定义字段和自定义数据
   */
  fun filterCustomStr(
    entryV4: PwEntryV4,
    needAddCustomData: Boolean = true
  ): Map<String, ProtectedString> {
    val map = HashMap<String, ProtectedString>()
    var addOTPPass = false
    for (str in entryV4.strings) {
      if (str.key.equals(PwEntryV4.STR_NOTES, true)
        || str.key.equals(PwEntryV4.STR_PASSWORD, true)
        || str.key.equals(PwEntryV4.STR_TITLE, true)
        || str.key.equals(PwEntryV4.STR_URL, true)
        || str.key.equals(PwEntryV4.STR_USERNAME, true)
      ) {
        continue
      }

      map[str.key] = str.value

      // 增加TOP密码字段
      if (!addOTPPass && (str.key.startsWith("TOTP", ignoreCase = true)
          || str.key.startsWith("OTP", ignoreCase = true))
      ) {
        addOTPPass = true
        val totpPass = OtpUtil.getOtpPass(entryV4)
        if (TextUtils.isEmpty(totpPass.second)) {
          continue
        }
        val totpPassStr = ProtectedString(true, totpPass.second)
        totpPassStr.isOtpPass = true
        map["TOTP"] = totpPassStr
      }
    }


    if (needAddCustomData) {
      for (str in entryV4.customData) {
        map[str.key] = ProtectedString(false, str.value)
      }
    }

    return map.toList()
      .sortedBy { it.first }
      .toMap()
  }

  /**
   * 格式化时间
   */
  @SuppressLint("SimpleDateFormat")
  fun formatTime(time: Date?): String {
    val format = SimpleDateFormat(" yyyy/MM/dd HH:mm")
    if (time == null) {
      return format.format(GregorianCalendar(1970, 1, 1, 0, 0, 0))
    }

    return format.format(time)
  }

  @SuppressLint("SimpleDateFormat")
  fun formatTime(
    time: Date,
    format: String
  ): String {
    return SimpleDateFormat(format).format(time)
  }

  /**
   * 跳转群组详情或项目详情
   */
  fun turnEntryDetail(
    activity: FragmentActivity,
    entry: PwDataInf,
    showElement: View? = null
  ) {
    if (entry is PwGroup) {
      Routerfit.create(ActivityRouter::class.java, activity).toGroupDetailActivity(
        groupName = entry.name,
        groupId = entry.id,
        opt = ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
      )
      return
    }

    if (entry is PwEntry) {
      val opt = if (showElement != null) {
        val pair = androidx.core.util.Pair(
          showElement, activity.getString(R.string.transition_entry_icon)
        )
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pair)
      } else {
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
      }
      Routerfit.create(ActivityRouter::class.java, activity).toEntryDetailActivity(
        entryId = entry.uuid,
        groupName = entry.parent.name,
        opt = opt
      )
    }
  }

  /**
   * @param requestCode 请求码
   * @param obj activity 或 fragment
   * @param type mime
   *
   * @see <a href="https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Complete_list_of_MIME_types">mime</a>
   */
  fun openSysFileManager(
    obj: Any,
    type: String,
    requestCode: Int
  ) {
    try {
      Intent.ACTION_GET_CONTENT
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        this.type = type
        addCategory(Intent.CATEGORY_OPENABLE)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

      }
      if (obj is Activity) {
        obj.startActivityForResult(intent, requestCode)
      } else if (obj is Fragment) {
        obj.startActivityForResult(intent, requestCode)
      }
    } catch (e: Exception) {
      Timber.e("打开文件失败")
      Timber.e(e)
    }
  }

  /**
   * 使用ASF创建文件
   * @param obj activity 或 fragment
   * @param mimeType mime
   * @see <a href="https://developer.android.com/guide/topics/providers/document-provider?hl=zh-cn#create">创建文档</a>
   */
  @Deprecated("请使用registerForActivityResult(ActivityResultContracts.CreateDocument())")
  fun createFile(
    obj: Any,
    mimeType: String,
    fileName: String,
    requestCode: Int
  ) {
    try {
      val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = mimeType
      intent.putExtra(Intent.EXTRA_TITLE, fileName)
      if (obj is Activity) {
        obj.startActivityForResult(intent, requestCode)
      } else if (obj is Fragment) {
        obj.startActivityForResult(intent, requestCode)
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  /**
   * 打开或隔壁键盘
   */
  fun toggleKeyBord(context: Context) {
    val imm = getSystemService(context, InputMethodManager::class.java)
    imm!!.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
  }

  /**
   * 从uri中获取文件长度
   */
  fun getFileSizeFormUri(
    context: Context,
    uri: Uri
  ): Long {
    if ("content".equals(uri.scheme, false)) {
      val df = DocumentFile.fromSingleUri(context, uri)

      if (df != null) {
        return df.length()
      }

      if (!UriUtil.checkPermissions(context, uri)) {
        Timber.e("uri没有授权：$uri")
        return 0
      }

      val cursor = context.contentResolver.query(uri, null, null, null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))

        cursor.close()
        return size
      }
    } else if (uri.scheme.equals("file", false)) {
      return File(uri.path).length()
    }

    return 0
  }

  /**
   * 从uri 中获取文件的路径，没用!!
   * 该方法的拷贝地址：https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri
   * 如果只是想使用流，可以使用 contentResolver.openOutputStream(uri) 获取流
   */
  @SuppressLint("ObsoleteSdkInt") fun getFilePathFormUri(
    context: Context,
    uri: Uri
  ): String? {
    var tempUri = uri
    val needToCheckUri = VERSION.SDK_INT >= 19
    var selection: String? = null
    var selectionArgs: Array<String>? = null
    Timber.d("uri = $uri")
    if (needToCheckUri && DocumentsContract.isDocumentUri(context.applicationContext, tempUri)) {
      when {
        isExternalStorageDocument(tempUri) -> {
          val docId = DocumentsContract.getDocumentId(tempUri)
          val split = docId.split(":")
            .toTypedArray()
          return Environment.getExternalStorageDirectory()
            .toString() + "/" + split[1]
        }
        isDownloadsDocument(tempUri) -> {
          val id = DocumentsContract.getDocumentId(tempUri)
          if (id.startsWith("raw", ignoreCase = true)) {
            val temp = Uri.parse(id)
//              val path = temp.path
//              val s = temp.scheme
            return temp.path
          } else if (isNumeric(id)) {
            tempUri = ContentUris.withAppendedId(
              Uri.parse("content://downloads/public_downloads"),
              id.toLong()
            )
          } else if (id.startsWith("msf", ignoreCase = true)) {
            // android 10 的问题，一样有问题！！
            tempUri = ContentUris.withAppendedId(
              Uri.parse("content://downloads/public_downloads"), id.split(":")[1].toLong()
            )
            Timber.d("msf Uri = $tempUri")
          }
        }
        isMediaDocument(tempUri) -> {
          val docId = DocumentsContract.getDocumentId(tempUri)
          val split = docId.split(":")
            .toTypedArray()
          val type = split[0]
          if ("image" == type) {
            tempUri = Media.EXTERNAL_CONTENT_URI
          } else if ("video" == type) {
            tempUri = Video.Media.EXTERNAL_CONTENT_URI
          } else if ("audio" == type) {
            tempUri = Audio.Media.EXTERNAL_CONTENT_URI
          }
        }
      }
    }
    if ("content".equals(tempUri.scheme, ignoreCase = true)) {
      val projection = arrayOf(Media.DATA)
      var url = ""
      val cursor: Cursor?
      try {
        cursor = context.contentResolver
          .query(uri, projection, selection, selectionArgs, null)
        if (cursor == null) {
          return null
        }
        val columnIndex: Int = cursor.getColumnIndexOrThrow(Media.DATA)

        if (cursor.moveToFirst()) {
          url = cursor.getString(columnIndex)
        }
        cursor.close()
      } catch (e: Exception) {
      }
      return url
    } else if ("file".equals(tempUri.scheme, ignoreCase = true)) {
      return tempUri.path
    }
    return null
  }

  fun isNumeric(str: String?): Boolean {
    val bigStr: String = try {
      BigDecimal(str).toString()
    } catch (e: java.lang.Exception) {
      return false //异常 说明包含非数字。
    }
    return true
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
  }
}

fun Uri.getFileInfo(
  context: Context
): Pair<String?, Long?> {
  return Pair(
    UriUtil.getFileNameFromUri(context, this),
    KeepassAUtil.instance.getFileSizeFormUri(context, this)
  )
}

fun PwEntry.isRef(): Boolean {
  return (!username.isNullOrEmpty() && username.startsWith("{REF:", ignoreCase = true))
    || (!password.isNullOrEmpty() && password.startsWith("{REF:", ignoreCase = true))
}

/**
 * 是否是Otp字段
 */
fun String.isOtp(): Boolean {
  return this.equals("totp", ignoreCase = true) || this.equals("otp", ignoreCase = true)
}

/**
 * uri 授权
 */
fun Uri.takePermission() {
  try {
    val takeFlags =
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    BaseApp.APP.contentResolver.takePersistableUriPermission(this, takeFlags)
  } catch (e: Exception) {
    HitUtil.toaskShort(BaseApp.APP.getString(R.string.error_uri_grant_permission))
    Timber.e(e)
  }
}
