package com.lyy.keepassa.util.cloud

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.view.View
import com.arialyy.frame.util.SharePreUtil
import com.arialyy.frame.util.StringUtil
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.helper.KDBHandlerHelper
import com.lyy.keepassa.R
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.MsgDialog.OnBtClickListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

/**
 * 数据库同步工具
 */
object DbSynUtil : SynStateCode {

  private val TAG = StringUtil.getClassName(this)
  private val KEY_SERVICE_MODIFY_TIME = "KEY_SERVICE_MODIFY_TIME"

  /**
   * 打开数据库时，记录的云盘时间，上传完成需要重新更新
   */
  var serviceModifyTime: Date = Date(System.currentTimeMillis())

  init {
    serviceModifyTime =
      Date(SharePreUtil.getLong(Constance.PRE_FILE_NAME, BaseApp.APP, KEY_SERVICE_MODIFY_TIME))
  }

  /**
   * 或去该记录在云端的修改时间
   */
  suspend fun getFileServiceModifyTime(record: DbRecord): Date {
    return CloudUtilFactory.getCloudUtil(record.getDbPathType())
        .getFileServiceModifyTime(record.cloudDiskPath!!)
  }

  /**
   * 更新服务器端文件的修改时间
   */
  suspend fun updateServiceModifyTime(
    record: DbRecord
  ) {
    serviceModifyTime = CloudUtilFactory.getCloudUtil(record.getDbPathType())
        .getFileServiceModifyTime(record.cloudDiskPath!!)
    SharePreUtil.putLong(
        Constance.PRE_FILE_NAME,
        BaseApp.APP, KEY_SERVICE_MODIFY_TIME, serviceModifyTime.time
    )
    Log.d(
        TAG, "更新云端文件修改时间：${KeepassAUtil.formatTime(serviceModifyTime)}"
    )
  }

  /**
   * 从云端下载的文件缓存路径
   * @param cloudTypeName 云端网盘名
   */
  fun getCloudDbTempPath(
    cloudTypeName: String,
    dbName: String
  ): Uri {
    val file = File("${BaseApp.APP.cacheDir.path}/$cloudTypeName/${dbName}")
    if (!file.parentFile.exists()) {
      file.parentFile.mkdirs()
    }
    return Uri.fromFile(file)
  }

  /**
   * 上传同步
   */
  suspend fun uploadSyn(
    context: Context,
    record: DbRecord
  ): Int {
    if (BaseApp.isAFS()) {
      Log.i(TAG, "AFS 不需要上传")
      return STATE_SUCCEED
    }
    HitUtil.toaskShort(BaseApp.APP.getString(R.string.start_upload_db))
    Log.d(TAG, "上传文件：${record.getDbUri()}，云盘路径：${record.cloudDiskPath}")
    val util = CloudUtilFactory.getCloudUtil(record.getDbPathType())
    val cloudFileInfo = util.getFileInfo(record.cloudDiskPath!!)
    Log.i(TAG, "获取文件信息成功：${cloudFileInfo.toString()}")
    if (cloudFileInfo == null) {
      Log.i(TAG, "云端文件不存在，开始上传文件")
      return uploadFile(util, context, record)
    } else {
      if (cloudFileInfo.contentHash != null) {
        if (util.checkContentHash(cloudFileInfo.contentHash, record.getDbUri())) {
          Log.i(TAG, "云端文件和本地文件的hash一致，忽略该上传")
          return STATE_SUCCEED
        }
      }
      Log.i(TAG, "云端文件存在，开始同步数据")
      return synUploadFile(util, context, record)
    }
  }

  /**
   * 只用于下载，如果文件存在，先会删除文件，再执行下载
   */
  suspend fun downloadOnly(
    context: Context,
    dbRecord: DbRecord,
    filePath: Uri
  ): String? {
    Log.i(TAG, "开始下载文件，云端路径：${dbRecord.cloudDiskPath}，文件保存路径：${filePath}")
    val util = CloudUtilFactory.getCloudUtil(DbPathType.valueOf(dbRecord.type))
    val path = util.downloadFile(context, dbRecord, filePath)
    if (!TextUtils.isEmpty(path)) {
      updateServiceModifyTime(dbRecord)
    }
    return path
  }

  /**
   * 下载同步
   */
  suspend fun downloadSyn(
    context: Context,
    record: DbRecord,
    filePath: Uri
  ): Int {
    if (BaseApp.isAFS()) {
      Log.i(TAG, "AFS 不需要下载")
      return STATE_SUCCEED
    }
    val util = CloudUtilFactory.getCloudUtil(record.getDbPathType())
    if (serviceModifyTime == util.getFileServiceModifyTime(record.cloudDiskPath!!)) {
      Log.i(TAG, "云端文件没有修改")
      return STATE_SUCCEED
    }

    val path = util.downloadFile(context, record, filePath)
    if (path == null) {
      Log.e(TAG, "下载文件失败，${record.cloudDiskPath}")
      toask(context.getString(R.string.sync_db), false, context.getString(R.string.net_error))
      return STATE_DOWNLOAD_FILE_FAIL
    }
    val kdb = openDb(context, dbPath = path)
    if (kdb == null) {
      Log.e(TAG, "打开云端数据库失败，将覆盖云端数据库")
      return coverFile(util, context, record)
    }
    return compareDb(util, context, record, kdb, BaseApp.KDB.pm, false)
  }

  /**
   * 上传同步文件操作
   */
  private suspend fun synUploadFile(
    util: ICloudUtil,
    context: Context,
    record: DbRecord
  ): Int {
    val st = util.getFileServiceModifyTime(record.cloudDiskPath!!)
    if (st == serviceModifyTime) {
      Log.i(
          TAG,
          "云端文件修改时间:${KeepassAUtil.formatTime(st)} 和本地缓存的云端文件时间:${KeepassAUtil.formatTime(
              serviceModifyTime
          )} 一致，开始覆盖云端文件"
      )
      return coverFile(util, context, record)
    }

    Log.i(
        TAG,
        "云端文件修改时间:${KeepassAUtil.formatTime(st)} 和本地缓存的云端文件时间:${KeepassAUtil.formatTime(
            serviceModifyTime
        )} 不一致，开始下载云端文件"
    )

    // 下载临时文件
    val filePath = getCloudDbTempPath(
        record.type, "kpa_${StringUtil.keyToHashKey(record.cloudDiskPath)}.kdbx"
    )
    val path = util.downloadFile(context, record, filePath)
    if (TextUtils.isEmpty(path)) {
      Log.e(TAG, "下载文件失败，${record.cloudDiskPath}")
      toask(context.getString(R.string.sync_db), false, context.getString(R.string.net_error))
      return STATE_DOWNLOAD_FILE_FAIL
    }

    val db = File(path)
    Log.i(TAG, "云端文件下载成功，开始打开数据库，filePath = ${db.path}，fileSize = ${db.length()}")
    val kdb = openDb(context, dbPath = path!!)
    if (kdb == null) {
      Log.e(TAG, "打开云端数据库失败，将覆盖云端数据库")
      return coverFile(util, context, record)
    }

    Log.i(TAG, "打开云端数据库成功，开始比对数据")
    return compareDb(util, context, record, kdb, BaseApp.KDB.pm, true)
  }

  /**
   * 对比云端和本地的数据库，并进行合并
   * @param isUpload 是否是上传
   */
  @ExperimentalCoroutinesApi
  private suspend fun compareDb(
    util: ICloudUtil,
    context: Context,
    record: DbRecord,
    cloudDb: PwDatabase,
    localDb: PwDatabase,
    isUpload: Boolean
  ): Int {
    val modifyList = ArrayList<Pair<PwDataInf, PwDataInf>>() // 有改动的条目，first 为云端的条目，second 为本地的条目
    val delList = ArrayList<PwDataInf>() // 云端没有的条目
    val newList = ArrayList<PwDataInf>() // 本地没有的条目
    val moveList = ArrayList<PwDataMap>() // 被移动的条目，first 为云端的条目，second 为本地的条目

    for (cloudEntry in cloudDb.entries.values) {
      val localEntry = localDb.entries[cloudEntry.uuid]
      when {
        localEntry == null -> {
          newList.add(cloudEntry)
        }
        cloudEntry.parent.id != localEntry.parent.id -> {
          moveList.add(PwDataMap(cloudEntry, localEntry))
        }
        localDb.entries[cloudEntry.uuid] == null -> {
          delList.add(cloudEntry)
        }
        cloudEntry != localEntry -> {
          Log.d(TAG, "修改的条目：${cloudEntry.title}")
          modifyList.add(Pair(cloudEntry, localEntry))
        }
      }
    }

    for (cloudGroup in cloudDb.groups.values) {
      val localGroup = localDb.groups[cloudGroup.id]

      when {
        localGroup == null -> {
          newList.add(cloudGroup)
        }
        cloudGroup.parent == null -> {
          Log.w(TAG, "云端数据库的群组的parent为空，群组名：${cloudGroup.name}")
        }
        cloudGroup.parent.id != localGroup.parent.id -> {
          moveList.add(PwDataMap(cloudGroup, localGroup))
        }
        localDb.groups[cloudGroup.id] == null -> {
          delList.add(cloudGroup)
        }
        cloudGroup != localGroup -> {
          Log.d(TAG, "修改的群组：${cloudGroup.name}")
          modifyList.add(Pair(cloudGroup, localGroup))
        }
      }
    }

    Log.i(
        TAG,
        "比对数据完成，newListSize = ${newList.size}，moveListSize = ${moveList.size}，delListSize = ${delList.size}，modifyListSize = ${modifyList.size}"
    )

    if (newList.size == 0 && moveList.size == 0 && delList.size == 0 && modifyList.size == 0) {
      Log.i(TAG, "对比结果：无新增，无删除，无移动，无修改，忽略该次上传，并更新缓存的云端文件修改时间")
      updateServiceModifyTime(record)
      return STATE_SUCCEED
    }

    if (newList.size > 0) {
      // 本地需要新增的条目
      Log.i(TAG, "本地需要新增条目")
      localAddNewEntry(newList, localDb)
    }

    if (moveList.size > 0) {
      // 本地需要移动的条目
      Log.i(TAG, "本地需要移动条目")
      moveLocalEntry(moveList, localDb)
    }

    if (modifyList.size <= 0) {
      val code = KdbUtil.saveDb(uploadDb = false, isSync = true)
      Log.i(TAG, "没有冲突的条目，保存数据库${if (code == STATE_SUCCEED) "成功" else "失败"}")
      return code
    }

    // 有改动提示用户合并数据
    Log.i(TAG, "有改动提示用户合并数据")
    var code = STATE_FAIL
    val channel = Channel<Int>()
    if (isUpload) {
      showUploadCoverDialog(context, record, util, modifyList, channel)
    } else {
      showDownloadCoverDialog(modifyList, channel)
    }

    val job = GlobalScope.launch {
      code = channel.receive()
      Log.d(TAG, "xxxxx = $code")
    }
    //  等待直到子协程执行结束，完美替换wait single
    job.join()
    job.cancel()
    channel.cancel()
    Log.d(TAG, "compareDb end point, code = $code")
    return code
  }

  /**
   * 移动数据
   * @param needMoveList 本地需要移动的数据，first 为云端的条目，second 为本地的条目
   */
  private fun moveLocalEntry(
    needMoveList: ArrayList<PwDataMap>,
    localDb: PwDatabase
  ) {
    for (p in needMoveList) {
      if (p.cloudPwData is PwEntry) {
        localDb.moveEntry(p.localPwData as PwEntry, getParentByCloudPwData(p.cloudPwData, localDb))
        continue
      }
      // 处理群组的移动
      localDb.moveGroup(p.localPwData as PwGroup, getParentByCloudPwData(p.cloudPwData, localDb))
    }
  }

  /**
   * 通过云端条目获取本地条目
   */
  private fun getParentByCloudPwData(
    cloudPwDataInf: PwDataInf,
    localDb: PwDatabase
  ): PwGroup {
    var parent = localDb.rootGroup

    if (cloudPwDataInf.parent != null) {
      val temp = localDb.groups[cloudPwDataInf.parent.id]
      if (temp != null) {
        parent = temp
      }
    }
    return parent
  }

  /**
   * 本地新增云端有而本地没的条目
   * @param newList 云端服务器新增加的条目列表
   */
  private suspend fun localAddNewEntry(
    newList: ArrayList<PwDataInf>,
    localDb: PwDatabase
  ) {
    // 需要先增加群组
    for (pwData in newList) {
      if (pwData is PwGroup) {
        val newGroup = pwData.clone()
        newGroup.childGroups?.clear()
        newGroup.childEntries?.clear()
        newGroup.parent = getParentByCloudPwData(pwData, localDb)
        KdbUtil.addGroup(newGroup)
      }
    }
    // 再增加条目
    for (pwData in newList) {
      if (pwData is PwEntry) {
        val newEntry = pwData.clone(true)
        newEntry.parent = getParentByCloudPwData(pwData, localDb)
        KdbUtil.addEntry(newEntry, save = false, uploadDb = false)
      }
    }
  }

  /**
   * 显示下载文件时冲突的对话框
   */
  @ExperimentalCoroutinesApi
  private fun showDownloadCoverDialog(
    modifyList: ArrayList<Pair<PwDataInf, PwDataInf>>,
    channel: Channel<Int>
  ) {
    val sb = StringBuilder()
    for (p in modifyList) {
      if (p.second is PwEntry) {
        sb.append((p.second as PwEntry).title)
      } else {
        sb.append((p.second as PwGroup).name)
      }
      sb.append("\n")
    }

    val res = BaseApp.APP.resources
    val dialog = MsgDialog.generate {
      msgTitle = res.getString(R.string.warning)
      msgContent = res.getString(R.string.file_conflict_msg, sb.toString())
      showCancelBt = true
      showCoverBt = false
      interceptBackKey = true
      build()
    }
    dialog.setEnterBtText(res.getString(R.string.cover_local))
    dialog.setOnBtClickListener(object : OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        GlobalScope.launch {

          // 覆盖本地数据
          if (type == MsgDialog.TYPE_ENTER) {
            channel.send(coverModifyEntry(modifyList))
            return@launch
          }
          channel.send(STATE_SUCCEED)
        }
      }
    })
    dialog.show()
  }

  /**
   * 显示上传文件冲突对话框
   * @param modifyList 有改动的条目，first 为云端的条目，second 为本地的条目
   */
  @ExperimentalCoroutinesApi
  private fun showUploadCoverDialog(
    context: Context,
    dbRecord: DbRecord,
    util: ICloudUtil,
    modifyList: ArrayList<Pair<PwDataInf, PwDataInf>>,
    channel: Channel<Int>
  ) {
    val sb = StringBuilder()
    for (p in modifyList) {
      if (p.second is PwEntry) {
        sb.append((p.second as PwEntry).title)
      } else {
        sb.append((p.second as PwGroup).name)
      }
      sb.append("\n")
    }
    val res = BaseApp.APP.resources
    val dialog = MsgDialog.generate {
      msgTitle = res.getString(string.warning)
      msgContent = res.getString(string.file_conflict_msg, sb.toString())
      showCancelBt = false
      showCoverBt = true
      interceptBackKey = true
      build()
    }
    dialog.setEnterBtText(res.getString(string.cover_local))
    dialog.setCoverBtText(res.getString(string.cover_cloud))
    dialog.setOnBtClickListener(object : OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        GlobalScope.launch {
          // 覆盖本地数据
          if (type == MsgDialog.TYPE_ENTER) {
            coverModifyEntry(modifyList)
            channel.send(coverFile(util, context, dbRecord))
            return@launch
          }

          // 覆盖云端数据
          if (type == MsgDialog.TYPE_COVER) {
            channel.send(coverFile(util, context, dbRecord))
            return@launch
          }
          channel.send(STATE_SUCCEED)
        }
      }
    })
    dialog.show()
    Log.d(TAG, "showUploadCoverDialog endPoint")
  }

  /**
   * 覆盖本地数据库有修改冲突的条目和群组
   * @param modifyList 有改动的条目，first 为云端的条目，second 为本地的条目
   */
  private suspend fun coverModifyEntry(modifyList: ArrayList<Pair<PwDataInf, PwDataInf>>): Int {
    for (p in modifyList) {
      if (p.first is PwEntry) {
        (p.second as PwEntry).assign(p.first as PwEntry)
      } else {
        (p.second as PwGroup).assign(p.first as PwGroup)
      }
    }

    val code = KdbUtil.saveDb(uploadDb = false, isSync = true)
    Log.i(TAG, "保存数据库${if (code == STATE_SUCCEED) "成功" else "失败"}")
    return code
  }

  /**
   * 打开数据库
   */
  private fun openDb(
    context: Context,
    dbPath: String
  ): PwDatabase? {
    val uri = Uri.parse(dbPath)
    Log.i(TAG, "dbUri = $uri")
    val temp = KDBHandlerHelper.getInstance(context)
        .openDb(
            uri, QuickUnLockUtil.decryption(BaseApp.dbPass),
            if (TextUtils.isEmpty(BaseApp.dbKeyPath)) null else Uri.parse(
                QuickUnLockUtil.decryption(BaseApp.dbKeyPath)
            )
        )
    if (temp?.pm == null) {
      return null
    }
    return temp.pm
  }

  /**
   * 覆盖文件，webdav 不需要删除
   */
  private suspend fun coverFile(
    util: ICloudUtil,
    context: Context,
    record: DbRecord
  ): Int {
    val needDelFile = when (record.getDbPathType()) {
      DROPBOX -> true
      WEBDAV -> false
      else -> false
    }
    // 处理需要删除文件的情况
    if (needDelFile) {
      if (util.delFile(record.cloudDiskPath!!)) {
        Log.i(TAG, "删除云端文件成功：${record.cloudDiskPath}")
        return uploadFile(util, context, record)
      }

      Log.e(TAG, "删除云端文件失败：${record.cloudDiskPath}")
      return STATE_DEL_FILE_FAIL
    }

    return uploadFile(util, context, record)
  }

  /**
   * 上传文件
   */
  private suspend fun uploadFile(
    util: ICloudUtil,
    context: Context,
    record: DbRecord
  ): Int {
    val b = util.uploadFile(context, record)
    Log.d(TAG, "上传文件${if (b) "成功" else "失败"}：${record.cloudDiskPath}")
    return if (b) STATE_SUCCEED else STATE_FAIL
  }

  private fun toask(
    msg: String,
    success: Boolean,
    des: String
  ) {
    BaseApp.handler.post {
      HitUtil.toaskShort(
          "$msg ${if (success) BaseApp.APP.getString(R.string.success) else BaseApp.APP.getString(
              R.string.fail
          )} $des"
      )
    }
  }

  private data class PwDataMap(
    val cloudPwData: PwDataInf,
    val localPwData: PwDataInf
  )

}