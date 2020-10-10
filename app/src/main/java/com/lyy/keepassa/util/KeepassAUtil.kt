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
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ActivityOptions
import android.app.assist.AssistStructure
import android.content.ContentUris
import android.content.Context
import android.content.Intent
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
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.arialyy.frame.core.AbsFrame
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.service.multidatasetservice.AutoFillHelper
import com.lyy.keepassa.service.multidatasetservice.StructureParser
import com.lyy.keepassa.view.create.CreateDbActivity
import com.lyy.keepassa.view.detail.EntryDetailActivity
import com.lyy.keepassa.view.detail.GroupDetailActivity
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.launcher.OpenDbHistoryActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import com.lyy.keepassa.view.search.AutoFillEntrySearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

object KeepassAUtil {

  const val TAG = "KeepassAUtil"
  private var LAST_CLICK_TIME = System.currentTimeMillis()

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
    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    BaseApp.APP.contentResolver.takePersistableUriPermission(this, takeFlags)
  }

  /**
   * 是否需要启动快速解锁
   * @return true 启动快速解锁
   */
  fun isStartQuickLockActivity(obj: Any): Boolean {
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
    val isOpenQuickLock = PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
        .getBoolean(context.getString(R.string.set_quick_unlock), false)
    if (BaseApp.KDB == null || !isOpenQuickLock) {
      context.startActivity(Intent(context, LauncherActivity::class.java).apply {
        putExtra(LauncherActivity.KEY_OPEN_TYPE, LauncherActivity.OPEN_TYPE_OPEN_DB)
      })
      for (ac in AbsFrame.getInstance().activityStack) {
        if (ac is LauncherActivity) {
          continue
        }
        ac.finish()
      }
    } else {
      context.startActivity(Intent(context, QuickUnlockActivity::class.java))
    }

  }

  /**
   * 回到启动页
   * @param turnFragment [LauncherActivity.OPEN_TYPE_CHANGE_DB]选择数据库页面，
   * [LauncherActivity.OPEN_TYPE_OPEN_DB] 打开数据库
   */
  fun turnLauncher(
    context: Context,
    turnFragment: Int = LauncherActivity.OPEN_TYPE_CHANGE_DB
  ) {
    BaseApp.isLocked = true
    context.startActivity(Intent(context, LauncherActivity::class.java).apply {
      putExtra(LauncherActivity.KEY_OPEN_TYPE, turnFragment)
    })
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
  fun checkUrlIsValid(url: String): Boolean {
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
    item.subTitle = KdbUtil.getUserName(entry)
    item.obj = entry
    return item
  }

  /**
   * 将pwGroup 转换为列表实体
   */
  fun convertPwGroup2Item(
    context: Context,
    pwGroup: PwGroup
  ): SimpleItemEntity {
    val item = SimpleItemEntity()
    item.title = pwGroup.name
    item.subTitle = context.getString(
        R.string.hint_group_desc, KdbUtil.getGroupEntryNum(pwGroup)
        .toString()
    )
    item.obj = pwGroup
    return item
  }

  /**
   * 保存上一次打开的数据库记录
   */
  fun saveLastOpenDbHistory(record: DbRecord) {
    GlobalScope.launch(Dispatchers.IO) {
      val dao = BaseApp.appDatabase.dbRecordDao()
      val his = dao.findRecord(record.localDbUri)
      if (his == null || his.localDbUri.isEmpty()) {
        record.uid = 0 // 保证uid能自增且不冲突
        record.time = System.currentTimeMillis()
        dao.saveRecord(record)
        BaseApp.dbRecord = record
        Log.d(TAG, "保存数据库打开记录成功")
      } else {
        his.keyUri = record.keyUri
        his.cloudDiskPath = record.cloudDiskPath
        his.type = record.type
        his.time = record.time
        dao.updateRecord(his)
        BaseApp.dbRecord = his
        Log.d(TAG, "更新数据库打开记录成功")
      }
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
    val structure = intent.getParcelableExtra<AssistStructure>(
        AutofillManager.EXTRA_ASSIST_STRUCTURE
    )
    val parser = StructureParser(structure)
    parser.parseForFill(true)
    val autofillFields = parser.autoFillFields

    val datas = KDBAutoFillRepository.getFilledAutoFillFieldCollection(apkPkgName)
    val response =
      AutoFillHelper.newResponse(context, true, autofillFields, datas, apkPkgName)

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
    val structure = intent.getParcelableExtra<AssistStructure>(
        AutofillManager.EXTRA_ASSIST_STRUCTURE
    )
    val parser = StructureParser(structure)
    parser.parseForFill(true)
    val autofillFields = parser.autoFillFields

    val response =
      AutoFillHelper.newResponse(context, true, autofillFields, arrayListOf(pwEntry), apkPkgName)

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

  /**
   * 判断本应用是否已经位于最前端
   *
   * @param context
   * @return 本应用已经位于最前端时，返回 true；否则返回 false
   */
  fun isRunningForeground(context: Context): Boolean {
    val activityManager: ActivityManager =
      context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcessInfoList: List<RunningAppProcessInfo> = activityManager.runningAppProcesses
    /**枚举进程 */
    for (appProcessInfo in appProcessInfoList) {
      if (appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
        if (appProcessInfo.processName == context.applicationInfo.processName) {
          return true
        }
      }
    }
    return false
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
    Log.i(TAG, "截取短密码，长度：$passLen，截取类型：$subType")
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
  fun formatTime(time: Date): String {
    val format = SimpleDateFormat(" yyyy/MM/dd HH:mm")
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
      val intent = Intent(activity, GroupDetailActivity::class.java)
      intent.putExtra(GroupDetailActivity.KEY_GROUP_ID, entry.id)
      intent.putExtra(GroupDetailActivity.KEY_TITLE, entry.name)
      activity.startActivity(
          intent, ActivityOptions.makeSceneTransitionAnimation(activity)
          .toBundle()
      )
    } else if (entry is PwEntry) {
      val intent = Intent(activity, EntryDetailActivity::class.java)
      intent.putExtra(EntryDetailActivity.KEY_GROUP_TITLE, entry.parent.name)
      intent.putExtra(EntryDetailActivity.KEY_ENTRY_ID, entry.uuid)
      if (showElement != null) {
        val pair =
          Pair<View, String>(showElement, activity.getString(R.string.transition_entry_icon))
        activity.startActivity(
            intent, ActivityOptions.makeSceneTransitionAnimation(activity, pair)
            .toBundle()
        )
      } else {
        activity.startActivity(
            intent, ActivityOptions.makeSceneTransitionAnimation(activity)
            .toBundle()
        )
      }
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
      Log.e(TAG, "打开文件失败");
      e.printStackTrace()
    }

  }

  /**
   * 使用ASF创建文件
   * @param obj activity 或 fragment
   * @param mimeType mime
   * @see <a href="https://developer.android.com/guide/topics/providers/document-provider?hl=zh-cn#create">创建文档</a>
   */
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
      Log.e(TAG, "创建文件失败")
      e.printStackTrace()
    }
  }

  /**
   * 打开或隔壁键盘
   */
  fun toggleKeyBord(context: Context) {
    val imm = getSystemService(context, InputMethodManager::class.java)
    imm!!.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
  }

  fun Uri.getFileInfo(
    context: Context
  ): Pair<String?, Long?> {
    return Pair(UriUtil.getFileNameFromUri(context, this), getFileSizeFormUri(context, this))
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
        Log.e(TAG, "uri没有授权：$uri")
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
    Log.d(TAG, "uri = $uri")
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
            Log.d(TAG, "msf Uri = $tempUri")
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