package com.lyy.keepassa.view.dialog

import android.content.Context
import androidx.lifecycle.liveData
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDavLoginModule : BaseModule() {

  /**
   * 检查登录状态
   * 如果是创建数据库，不考虑文件是否存在
   * 如果是打开云端数据，如果文件不存在，则表示登录失败
   */
  fun checkLogin(
    context: Context,
    uri: String,
    userName: String,
    pass: String,
    isCreateLogin: Boolean
  ) = liveData {
    val success = withContext(Dispatchers.IO) {
      var isSuccess = false
      try {
        if (isCreateLogin) {
          WebDavUtil.checkLogin(uri, userName, pass)
        } else {
          WebDavUtil.login(uri, userName, pass)
          val b = WebDavUtil.getFileInfo(uri) != null
          if (!b){
            HitUtil.toaskShort(context.getString(R.string.db_file_no_exist))
            return@withContext false
          }
        }

        // 保存记录
        val dao = BaseApp.appDatabase.cloudServiceInfoDao()
        var data = dao.queryServiceInfo(uri)
        if (data == null) {
          data = CloudServiceInfo(
              userName = QuickUnLockUtil.encryptStr(userName),
              password = QuickUnLockUtil.encryptStr(pass),
              cloudPath = uri
          )
          dao.saveServiceInfo(data)
        } else {
          data.userName = QuickUnLockUtil.encryptStr(userName)
          data.password = QuickUnLockUtil.encryptStr(pass)
          dao.updateServiceInfo(data)
        }
        isSuccess = true
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext isSuccess
    }
    emit(success)
  }

}