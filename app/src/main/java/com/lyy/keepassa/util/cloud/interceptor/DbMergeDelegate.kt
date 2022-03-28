package com.lyy.keepassa.util.cloud.interceptor

import android.util.Pair
import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwDataInf
import com.keepassdroid.database.PwDatabase
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwGroup
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.PwDataMap
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:42 下午 2021/12/24
 **/
object DbMergeDelegate {
  private val scope = MainScope()

  const val COVER_LOCAL = 999
  const val COVER_CLOUD = 998

  /**
   * 对比云端和本地的数据库，并进行合并
   * @param isUpload 是否是上传
   * @return [COVER_CLOUD]、[COVER_LOCAL]、[VS]
   */
  @ExperimentalCoroutinesApi
  suspend fun compareDb(
    record: DbHistoryRecord,
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
          Timber.d("修改的条目：${cloudEntry.title}")
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
          Timber.w("云端数据库的群组的parent为空，群组名：${cloudGroup.name}")
        }
        cloudGroup.parent.id != localGroup.parent.id -> {
          moveList.add(PwDataMap(cloudGroup, localGroup))
        }
        localDb.groups[cloudGroup.id] == null -> {
          delList.add(cloudGroup)
        }
        cloudGroup != localGroup -> {
          Timber.d("修改的群组：${cloudGroup.name}")
          modifyList.add(Pair(cloudGroup, localGroup))
        }
      }
    }

    Timber.i(
      "比对数据完成，newListSize = ${newList.size}，moveListSize = ${moveList.size}，delListSize = ${delList.size}，modifyListSize = ${modifyList.size}"
    )

    if (newList.size == 0 && moveList.size == 0 && delList.size == 0 && modifyList.size == 0) {
      Timber.i("对比结果：无新增，无删除，无移动，无修改，忽略该次上传，并更新缓存的云端文件修改时间")
      DbSynUtil.updateServiceModifyTime(record)
      return DbSynUtil.STATE_SUCCEED
    }

    if (newList.size > 0) {
      // 本地需要新增的条目
      Timber.i("本地需要新增条目")
      localAddNewEntry(newList, localDb)
    }

    if (moveList.size > 0) {
      // 本地需要移动的条目
      Timber.i("本地需要移动条目")
      moveLocalEntry(moveList, localDb)
    }

    if (modifyList.size <= 0) {
      KpaUtil.kdbHandlerService.saveDbByBackground()
      return DbSynUtil.STATE_SUCCEED
    }

    // 有改动提示用户合并数据
    Timber.i("有改动提示用户合并数据")
    var code = DbSynUtil.STATE_FAIL
    val channel = Channel<Int>()
    if (isUpload) {
      showUploadCoverDialog(modifyList, channel)
    } else {
      showDownloadCoverDialog(modifyList, channel)
    }

    val job = scope.launch {
      code = channel.receive()
    }
    //  等待直到子协程执行结束，完美替换wait single
    job.join()
    job.cancel()
    channel.cancel()
    Timber.d("compareDb end point, code = $code")
    return code
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
    Routerfit.create(DialogRouter::class.java).toMsgDialog(
      msgTitle = ResUtil.getString(R.string.warning),
      msgContent = res.getString(R.string.file_conflict_msg, sb.toString()),
      showCoverBt = false,
      showCancelBt = false,
      interceptBackKey = true,
      enterText = ResUtil.getString(R.string.cover_local),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onCover(v: Button) {
        }

        override fun onEnter(v: Button) {
          scope.launch {
            // 覆盖本地数据
            coverModifyEntry(modifyList)
            channel.send(COVER_LOCAL)
          }
        }

        override fun onCancel(v: Button) {
        }
      }
    ).show()
  }

  /**
   * 显示上传文件冲突对话框
   * @param modifyList 有改动的条目，first 为云端的条目，second 为本地的条目
   */
  @ExperimentalCoroutinesApi
  private fun showUploadCoverDialog(
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

    Routerfit.create(DialogRouter::class.java).toMsgDialog(
      msgTitle = ResUtil.getString(R.string.warning),
      msgContent = res.getString(R.string.file_conflict_msg, sb.toString()),
      showCancelBt = false,
      showCoverBt = true,
      interceptBackKey = true,
      enterText = ResUtil.getString(R.string.cover_local),
      coverText = ResUtil.getString(R.string.cover_cloud),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onCover(v: Button) {
          // 覆盖云端数据
          scope.launch {
            channel.send(COVER_CLOUD)
          }
        }

        override fun onEnter(v: Button) {
          scope.launch {
            // 覆盖本地数据
            coverModifyEntry(modifyList)
            channel.send(COVER_LOCAL)
          }
        }

        override fun onCancel(v: Button) {
        }
      }
    ).show()

    Timber.d("showUploadCoverDialog endPoint")
  }

  /**
   * 覆盖本地数据库有修改冲突的条目和群组
   * @param modifyList 有改动的条目，first 为云端的条目，second 为本地的条目
   */
  private fun coverModifyEntry(modifyList: ArrayList<Pair<PwDataInf, PwDataInf>>): Int {
    for (p in modifyList) {
      if (p.first is PwEntry) {
        (p.second as PwEntry).assign(p.first as PwEntry)
      } else {
        (p.second as PwGroup).assign(p.first as PwGroup)
      }
    }
    KpaUtil.kdbHandlerService.saveDbByBackground()
    return DbSynUtil.STATE_SUCCEED
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
}