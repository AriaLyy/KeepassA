package com.lyy.keepassa.view.setting

import android.content.Context
import android.net.Uri
import androidx.lifecycle.liveData
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 设置页面的module
 */
class SettingModule : BaseModule() {

  /**
   * 修改数据库密码
   */
  fun modifyDbName(newDbName: String) = liveData {

    val success = withContext(Dispatchers.IO) {
      try {
        BaseApp.KDB.pm.name = newDbName
        val code = KdbUtil.saveDb()
        if (code == DbSynUtil.STATE_SUCCEED) {
          BaseApp.dbName = newDbName
          return@withContext true
        }
        return@withContext false
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext false
    }
    emit(success)
  }

  /**
   * 修改密码
   */
  fun modifyPass(
    context: Context,
    newPass: String
  ) = liveData {
    val success = withContext(Dispatchers.IO) {
      try {
        if (BaseApp.dbKeyPath == null || BaseApp.dbKeyPath.isEmpty()) {
          BaseApp.KDB.pm.setMasterKey(newPass, null)
        } else {
          val ios = UriUtil.getUriInputStream(
              context, Uri.parse(QuickUnLockUtil.decryption(BaseApp.dbKeyPath))
          )
          BaseApp.KDB.pm.setMasterKey(newPass, ios)
        }
        return@withContext KdbUtil.saveDb() == DbSynUtil.STATE_SUCCEED
      } catch (e: Exception) {
        e.printStackTrace()
        HitUtil.toaskOpenDbException(e)
      }
      return@withContext false
    }
    emit(success)
  }

}