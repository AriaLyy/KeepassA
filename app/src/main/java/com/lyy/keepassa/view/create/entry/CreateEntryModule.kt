/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create.entry

import KDBAutoFillRepository
import android.content.Context
import android.graphics.Bitmap.CompressFormat.PNG
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.entity.CommonState.CREATE
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.entity.TagBean
import com.lyy.keepassa.event.AttrFileEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.getFileInfo
import com.lyy.keepassa.util.getRealUserName
import com.lyy.keepassa.util.hasNote
import com.lyy.keepassa.util.hasTOTP
import com.lyy.keepassa.util.cloud.DbSynUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 创建条目、群组的module
 */
class CreateEntryModule : BaseModule() {
  companion object {
    val attrFlow = MutableSharedFlow<AttrFileEvent>(0)
    val userNameFlow = MutableStateFlow<List<String>?>(null)
    private val userNameCache = arrayListOf<String>()
  }

  /**
   * 已经选中的标签
   */
  var selectedTagBeanCache = mutableListOf<String>()
  var customIcon: PwIconCustom? = null
  var icon = PwIconStandard(0)
  var autoFillParam: AutoFillParam? = null
  var strCacheMap = hashMapOf<String, ProtectedString>()
  var fileCacheMap = hashMapOf<String, ProtectedBinary>()
  lateinit var pwEntry: PwEntryV4

  fun updateEntryGroupIdAndSave(context: CreateEntryActivity, groupId: PwGroupIdV4) {
    Timber.i(
      "updateEntryGroupIdAndSave: invoked, groupId.id=%s, pwEntry.title=%s, pwEntry.uuid=%s, pwEntry.parent(before)=%s, pm.entries.size=%d",
      groupId.id,
      pwEntry.title,
      pwEntry.uuid,
      pwEntry.parent?.name,
      BaseApp.KDB.pm.entries.size
    )
    viewModelScope.launch {
      val targetGroup = KdbUtil.findV4GroupById(groupId.id)
      if (targetGroup == null) {
        Timber.e(
          "updateEntryGroupIdAndSave: chosen group %s not found in pm.groups nor tree walk. ABORT save (no fallback to root).",
          groupId.id
        )
        HitUtil.snackShort(
          context.rootView,
          ResUtil.getString(R.string.fail)
        )
        return@launch
      }

      val targetV4 = targetGroup as? PwGroupV4
      Timber.i(
        "updateEntryGroupIdAndSave: target found, name=%s, uuid=%s, isV4=%b, childEntries.size(before)=%d, in pm.groups=%b",
        targetGroup.name,
        targetV4?.uuid,
        targetV4 != null,
        targetGroup.childEntries.size,
        BaseApp.KDB.pm.groups.containsKey(targetGroup.id)
      )

      val beforeParent = pwEntry.parent?.name
      KpaUtil.kdbHandlerService.createEntry(pwEntry, targetGroup)

      val containsEntry = targetGroup.childEntries.contains(pwEntry)
      Timber.i(
        "updateEntryGroupIdAndSave: after createEntry, pwEntry.parent=%s (before=%s), target.childEntries.size=%d, target.childEntries.contains(pwEntry)=%b, pm.entries.size=%d",
        pwEntry.parent?.name,
        beforeParent,
        targetGroup.childEntries.size,
        containsEntry,
        BaseApp.KDB.pm.entries.size
      )

      if (!containsEntry) {
        Timber.e(
          "updateEntryGroupIdAndSave: pwEntry NOT in target.childEntries after createEntry, save aborted"
        )
        return@launch
      }

      KpaUtil.kdbHandlerService.saveOnly(true) { state ->
        if (shouldShowAutoFillSaveToast(
            isSucceed = state == DbSynUtil.STATE_SUCCEED,
            isAutoFillSave = autoFillParam?.isSave == true
        )) {
          HitUtil.toaskLong(ResUtil.getString(R.string.save_db_success))
        }
        context.finishAfterTransition()
      }
    }
  }

  fun initCache() {
    pwEntry.strings.forEach {
      strCacheMap[it.key] = it.value
    }
    pwEntry.binaries.forEach {
      fileCacheMap[it.key] = it.value
    }
    customIcon = pwEntry.customIcon ?: PwIconCustom.ZERO
    icon = pwEntry.icon
  }

  fun cacheTag(tagList: List<TagBean>) {
    selectedTagBeanCache.clear()
    selectedTagBeanCache.addAll(tagList.filter { it.isSet }.map {
      it.tag
    })
  }

  /**
   * 添加附件
   */
  fun addAttrFile(context: CreateEntryActivity, uri: Uri?) {
    val rootView = context.rootView
    if (uri == null) {
      Timber.e("附件uri为空")
      HitUtil.snackShort(
        rootView,
        "${ResUtil.getString(R.string.add_attr_file)}${ResUtil.getString(R.string.fail)}"
      )
      return
    }
    val fileInfo = uri.getFileInfo(context)
    if (TextUtils.isEmpty(fileInfo.first) || fileInfo.second == null) {
      Timber.e("获取文件名失败")
      HitUtil.snackShort(
        rootView,
        "${ResUtil.getString(R.string.add_attr_file)}${ResUtil.getString(R.string.fail)}"
      )
      return
    }
    val fileName = fileInfo.first!!
    val fileSize = fileInfo.second!!
    if (fileSize >= 1024 * 1024 * 10) {
      HitUtil.snackShort(rootView, ResUtil.getString(R.string.error_attr_file_too_large))
      return
    }
    val pbf = ProtectedBinary(
      false, UriUtil.getUriInputStream(context, uri)
        .readBytes()
    )
    (BaseApp.KDB.pm as PwDatabaseV4).binPool.poolAdd(pbf)
    context.lifecycleScope.launch {
      attrFlow.emit(AttrFileEvent(CREATE, fileName, pbf))
    }
  }

  /**
   * Traverse database and get all userName
   */
  suspend fun getUserNameCache() {
    if (userNameCache.isNotEmpty()) {
      userNameFlow.emit(userNameCache)
      return
    }

    val temp = hashSetOf<String>()

    withContext(Dispatchers.IO) {
      for (map in BaseApp.KDB.pm.entries) {
        if (map.value.username.isNullOrEmpty()) {
          continue
        }
        temp.add(map.value.getRealUserName())
      }
    }

    userNameCache.addAll(temp)
    userNameFlow.emit(userNameCache)
  }

  /**
   * 自动填充进行保存数据时，搜索条目信息，如果条目不存在，新建条目
   */
  fun getEntryFromAutoFillSave(
    context: Context,
    apkPkgName: String,
    userName: String?,
    pass: String?,
    domain: String? = null
  ): PwEntryV4 {
    val listStorage = ArrayList<PwEntry>()
    KdbUtil.searchEntriesByPackageName(apkPkgName, listStorage)
    val entry: PwEntryV4
    val autoFillParam = AutoFillParam(
      apkPkgName = apkPkgName,
      domain = domain,
      isSave = true
    )
    if (listStorage.isEmpty()) {
      entry = PwEntryV4(BaseApp.KDB.pm.rootGroup as PwGroupV4)
      AutoFillSaveEntryBinder.getWebUrl(autoFillParam)?.let {
        entry.setUrl(it, BaseApp.KDB.pm)
      } ?: AutoFillSaveEntryBinder.applyPackageAssociation(entry.strings, autoFillParam)

      val icon = IconUtil.getAppIcon(context, apkPkgName)
      if (icon != null) {
        val baos = ByteArrayOutputStream()
        icon.compress(PNG, 100, baos)
        val datas: ByteArray = baos.toByteArray()
        val customIcon = PwIconCustom(UUID.randomUUID(), datas)
        entry.customIcon = customIcon
        (BaseApp.KDB.pm as PwDatabaseV4).putCustomIcons(customIcon)
      }

      val appName = KDBAutoFillRepository.getAppName(context, apkPkgName)
      entry.setTitle(appName ?: "newEntry", BaseApp.KDB.pm)
      entry.icon = PwIconStandard(0)
    } else {
      entry = listStorage[0] as PwEntryV4
      Timber.w("已存在含有【$apkPkgName】的条目，将更新条目")
    }
    if (!userName.isNullOrEmpty()) {
      entry.setUsername(userName, BaseApp.KDB.pm)
    }
    if (!pass.isNullOrEmpty()) {
      entry.setPassword(pass, BaseApp.KDB.pm)
    }
    return entry
  }

  /**
   * 创建群组
   * @param groupName 群组名
   * @param parentGroup 父群组
   * @param icon 标准图标
   * @param customIcon 自定义图标
   */
  fun createGroup(
    groupName: String,
    parentGroup: PwGroupV4,
    icon: PwIconStandard,
    customIcon: PwIconCustom?,
    callback: (PwGroupV4) -> Unit
  ) {
    KpaUtil.kdbHandlerService.createGroup(groupName, icon, customIcon, parentGroup, callback)
  }

  /**
   * 构建的更多选择项目
   */
  fun getMoreItem(context: Context): ArrayList<SimpleItemEntity> {
    val list = ArrayList<SimpleItemEntity>()
    val titles = context.resources.getStringArray(R.array.v4_add_mor_item)
    val icons = context.resources.obtainTypedArray(R.array.v4_add_more_icon)
    val len = titles.size - 1
    for (i in 0..len) {
      val item = SimpleItemEntity()
      item.title = titles[i]
      item.icon = icons.getResourceId(i, 0)
      if (item.icon == R.drawable.ic_token_grey && pwEntry.hasTOTP()) {
        Timber.d("Already used totp")
        continue
      }
      if (item.icon == R.drawable.ic_notice && pwEntry.hasNote()) {
        Timber.d("Already used note")
        continue
      }
      list.add(item)
    }
    icons.recycle()
    return list
  }
}
