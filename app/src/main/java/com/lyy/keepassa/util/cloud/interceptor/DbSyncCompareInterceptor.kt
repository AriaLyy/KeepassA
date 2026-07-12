package com.lyy.keepassa.util.cloud.interceptor

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.arialyy.frame.util.ResUtil
import com.arialyy.frame.util.StringUtil
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.merge.DatabaseContentComparator
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.StorageType.WEBDAV
import timber.log.Timber
import java.io.File

/**
 * db compare
 * @Author laoyuyu
 * @Description
 * @Date 5:10 下午 2021/12/24
 **/
class DbSyncCompareInterceptor : IDbSyncInterceptor {
  override suspend fun intercept(request: DbSyncRequest): DbSyncResponse {
    val util = request.syncUtil
    val record = request.record

    val st = util.getFileServiceModifyTime(record.cloudDiskPath!!)
    if (st == DbSynUtil.serviceModifyTime) {
      Timber.i(
        "云端文件修改时间:${KeepassAUtil.instance.formatTime(st)} 和本地缓存的云端文件时间:${
          KeepassAUtil.instance.formatTime(
            DbSynUtil.serviceModifyTime
          )
        } 一致，开始校验云端与本地数据库内容"
      )
      val cloudDatabase = downloadCloudDatabase(request)
        ?: return error(DbSynUtil.STATE_DOWNLOAD_FILE_FAIL, "下载或打开云端数据库失败，${record.cloudDiskPath}")
      val localDatabase = BaseApp.KDB?.pm
        ?: return error(DbSynUtil.STATE_FAIL, "synUploadFile, local db is null")
      cloudDatabase.use { opened ->
        if (DatabaseContentComparator().sameContent(opened.database, localDatabase)) {
          Timber.i("云端与本地数据库内容一致，忽略该次上传")
          DbSynUtil.updateServiceModifyTime(record)
          return normal(DbSynUtil.STATE_SUCCEED, "云端与本地数据库内容一致，无需上传")
        }
      }
      Timber.i("云端时间未变化，但本地数据库内容有变化，开始上传")
      return coverFile(request)
    }

    Timber.i(
      "云端文件修改时间:${KeepassAUtil.instance.formatTime(st)} 和本地缓存的云端文件时间:${
        KeepassAUtil.instance.formatTime(
          DbSynUtil.serviceModifyTime
        )
      } 不一致，开始下载云端文件"
    )

    // 下载临时文件
    val filePath = DbSynUtil.getCloudDbTempPath(
      record.type, "kpa_${StringUtil.keyToHashKey(record.cloudDiskPath)}.kdbx"
    )
    val path = util.downloadFile(BaseApp.APP, record, filePath)
    if (path.isNullOrEmpty()) {
      DbSynUtil.toask(ResUtil.getString(string.sync_db), false, ResUtil.getString(string.net_error))
      return error(DbSynUtil.STATE_DOWNLOAD_FILE_FAIL, "下载文件失败，${record.cloudDiskPath}")
    }

    val db = DownloadedCloudFileResolver.resolve(path)
    Timber.i("云端文件下载成功，开始打开数据库，filePath = ${db.path}，fileSize = ${db.length()}")
    val kdb = openDb(BaseApp.APP, dbPath = path)
    if (kdb == null) {
      Timber.e("打开云端数据库失败，将覆盖云端数据库")
      return coverFile(request)
    }

    if (BaseApp.KDB?.pm == null) {
      return error(DbSynUtil.STATE_FAIL, "synUploadFile, local db is null")
    }
    Timber.i("打开云端数据库成功，开始比对数据")
    val code = kdb.use { opened ->
      DbMergeDelegate.compareDb(
        record,
        opened.database,
        BaseApp.KDB!!.pm,
        true,
        request.mergeFailureCallback,
        mergeInteractionMode = request.mergeInteractionMode,
        cloudSnapshot = db,
        cloudModifiedTime = st.time
      )
    }
    if (code != DbSynUtil.STATE_SUCCEED) {
      return error(code, "merge db fail")
    }
    return coverFile(request)
  }

  private suspend fun downloadCloudDatabase(request: DbSyncRequest): OpenedCloudDatabase? {
    val record = request.record
    val filePath = DbSynUtil.getCloudDbTempPath(
      record.type, "kpa_compare_${StringUtil.keyToHashKey(record.cloudDiskPath)}.kdbx"
    )
    val path = request.syncUtil.downloadFile(BaseApp.APP, record, filePath)
    if (path.isNullOrEmpty()) return null
    return openDb(BaseApp.APP, path)
  }

  private fun getNextRequest(request: DbSyncRequest): DbSyncRequest {
    return DbSyncRequest(
      record = request.record,
      syncUtil = request.syncUtil,
      interceptors = request.interceptors,
      index = request.index + 1,
      mergeFailureCallback = request.mergeFailureCallback,
      mergeInteractionMode = request.mergeInteractionMode
    )
  }

  /**
   * 覆盖文件，webdav 不需要删除
   */
  private suspend fun coverFile(request: DbSyncRequest): DbSyncResponse {
    val record = request.record
    val util = request.syncUtil
    val needDelFile = when (record.getDbPathType()) {
      DROPBOX -> true
      WEBDAV -> false
      else -> false
    }
    // 处理需要删除文件的情况
    if (needDelFile) {
      if (util.delFile(record.cloudDiskPath!!)) {
        Timber.i("删除云端文件成功：${record.cloudDiskPath}")
        return request.nextInterceptor()!!.intercept(getNextRequest(request))
      }
      return error(DbSynUtil.STATE_DEL_FILE_FAIL, "删除云端文件失败：${record.cloudDiskPath}")
    }

    return request.nextInterceptor()!!.intercept(getNextRequest(request))
  }

  /**
   * 打开数据库
   */
  private fun openDb(
    context: Context,
    dbPath: String
  ): OpenedCloudDatabase? {
    val uri = Uri.parse(dbPath)
    Timber.i("dbUri = $uri")
    val cloudContext = CloudDatabaseOpenContext.create(context)
    val temp = Database().apply {
      LoadDataStrict(
        cloudContext,
        uri,
        QuickUnLockUtil.decryption(BaseApp.dbPass),
        if (TextUtils.isEmpty(BaseApp.dbKeyPath)) null else Uri.parse(
          QuickUnLockUtil.decryption(BaseApp.dbKeyPath)
        )
      )
    }
    if (temp?.pm == null) {
      cloudContext.attachmentDirectory().deleteRecursively()
      return null
    }
    return OpenedCloudDatabase(temp, cloudContext)
  }
}
