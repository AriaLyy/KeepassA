package com.lyy.keepassa.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lyy.keepassa.entity.DbRecord
import java.util.concurrent.Executors

/**
 * 上传数据库的服务
 */
class UploadDbService : Service() {
  companion object {
    const val DATA_RECORD = "DATA_RECORD"
  }

  lateinit var dbRecord: DbRecord
  val pool = Executors.newSingleThreadExecutor()

  override fun onBind(intent: Intent?): IBinder? {

    return null
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    dbRecord = intent!!.getParcelableExtra(
        DATA_RECORD
    ) as DbRecord
    // 注意，服务中如果使用耗时任务是会阻塞的，因为服务也是运行在main中的，需要在服务器中开启线程执行任务，不要被服务的后台概念搞混了
    pool.submit(Task(this, dbRecord))
    return super.onStartCommand(intent, flags, startId)
  }

  private class Task(
    val context: UploadDbService,
    val dbRecord: DbRecord
  ) : Runnable {
    override fun run() {
//      when (DbPathType.valueOf(dbRecord.type)) {
//        DROPBOX -> {
//          val dbxUtil = DropboxUtil()
//          dbxUtil.uploadFile(context, dbRecord)
//        }
//      }
    }
  }

}