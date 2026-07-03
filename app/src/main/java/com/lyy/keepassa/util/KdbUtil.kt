/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.widget.TextView
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.FileUtil
import com.arialyy.frame.util.RegularRule
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.Utils
import com.keepassdroid.Database
import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupIdV3
import com.keepassdroid.database.PwGroupIdV4
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.totp.OtpUtil
import com.lyy.keepassa.widget.pb.RoundProgressBarWidthNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.util.UUID

object KdbUtil {

  val scope = MainScope()
  val txtArray = arrayListOf("txt", "md")
  val imgArray = arrayListOf("png", "jpg", "jpeg", "webp")
  val DATE_FORMAT = "yyyy/MM/dd HH:mm"

  /**
   * 定时自动获取otp密码
   */
  fun startAutoGetOtp(entryV4: PwEntryV4, rPb: RoundProgressBarWidthNumber, tvValue: TextView) {
    val p = OtpUtil.getOtpPass(entryV4)
    if (p.second.isNullOrEmpty()) {
      Timber.e("无法自动获取otp密码")
      return
    }
    rPb.setCountdown(true)

    scope.launch(Dispatchers.Main) {
      Timber.d(p.toString())
      val time = p.first
      rPb.max = time
      tvValue.text = p.second
      for (i in time downTo 1) {
        rPb.progress = i
        withContext(Dispatchers.IO) {
          delay(1000)
        }
      }
      startAutoGetOtp(entryV4, rPb, tvValue)
    }
  }

  fun openFile(fileName: String, file: ProtectedBinary) {
    txtArray.forEach {
      if (fileName.endsWith(it, true)) {
        Routerfit.create(DialogRouter::class.java).showMsgDialog(
          msgTitle = ResUtil.getString(R.string.txt_viewer),
          msgContent = String(file.data.readBytes()),
          showCancelBt = false
        )
        return
      }
    }
    imgArray.forEach {
      if (fileName.endsWith(it, true)) {
        val bytes = file.data.readBytes()
        if (bytes.isNotEmpty()) {
          Routerfit.create(DialogRouter::class.java).showImgViewerDialog(bytes)
        }
        return
      }
    }
    openFileBySystem(fileName, file)
  }

  private fun openFileBySystem(fileName: String, file: ProtectedBinary) {
    KpaUtil.scope.launch {
      val context = Utils.getApp()
      val targetFile = File(context.cacheDir, fileName)
      withContext(Dispatchers.IO) {
        val fic = Channels.newChannel(file.data)
        val foc = FileOutputStream(targetFile).channel
        foc.transferFrom(fic, 0, Int.MAX_VALUE.toLong())
        fic.close()
        foc.close()
      }
      FileUtil.openFile(context, targetFile)
    }
  }

  fun Database?.isNull(): Boolean {
    return this == null || this.pm == null
  }

  suspend fun getAllTags(): Set<String> {
    val tagList = hashSetOf<String>()
    withContext(Dispatchers.IO) {
      BaseApp.KDB.pm.entries.forEach {
        val entry = it.value as PwEntryV4
        if (entry.tags.isNullOrEmpty()) {
          return@forEach
        }
        tagList.addAll(getEntryTag(entry))
      }
    }
    return tagList
  }

  fun getEntryTag(entryV4: PwEntryV4): Set<String> {
    val tagSet = hashSetOf<String>()
    entryV4.tags.split(",").forEach {
      tagSet.add(it)
    }
    return tagSet
  }

  /**
   * 获取用户名，如果是引用其它条目的，解析其引用
   */
  @Deprecated(
    message = "please use pwEntry.getRealUserName()",
    ReplaceWith("getRealUserName()", "com.lyy.keepassa.util.getRealUserName")
  )
  fun getUserName(entry: PwEntry): String {
    return entry.getRealUserName()
  }

  /**
   * 获取密码，如果是引用其它条目的，解析其引用
   */
  @Deprecated(
    message = "please use pwEntry.getRealPass()",
    ReplaceWith("getRealPass()", "com.lyy.keepassa.util.getRealPass")
  )
  fun getPassword(entry: PwEntry): String {
    return entry.getRealPass()
  }

  /**
   * 通过搜索条目
   *
   * 域名匹配流程:
   *  1. 用项目自有的 [RegularRule] 提取输入域名的 top/second/third 三级候选(支持所有 TLD)。
   *  2. 加上输入域名本身,组成候选集。
   *  3. 条目的 URL 字段只要命中候选集中任一项,即加入结果。
   *
   * @param domain 域名(webDomain,如 ubits.club、login.example.com)
   * @param listStorage 搜索结果
   */
  fun searchEntriesByDomain(
    domain: String?,
    listStorage: MutableList<PwEntry>
  ) {
    if (domain.isNullOrEmpty()) {
      return
    }
    val candidates = linkedSetOf<String>()
    candidates.add(domain)
    Regex(RegularRule.DOMAIN_TOP, RegexOption.IGNORE_CASE).find(domain)?.value?.let(candidates::add)
    Regex(RegularRule.DOMAIN_SECOND, RegexOption.IGNORE_CASE).find(domain)?.value?.let(candidates::add)
    Regex(RegularRule.DOMAIN_THIRD, RegexOption.IGNORE_CASE).find(domain)?.value?.let(candidates::add)
    Timber.d("searchByDomain candidates = $candidates")
    for (entry in BaseApp.KDB.pm.entries.values) {
      val pe4 = entry as PwEntryV4
      val url = pe4.url
      val urlField = pe4.strings["URL"]?.toString()
      if (candidates.any { url.contains(it, ignoreCase = true) || urlField?.contains(it, ignoreCase = true) == true }) {
        listStorage.add(pe4)
      }
    }
  }

  /**
   * 通过包名搜索条目
   *
   * @param pkgName 包名
   * @param listStorage 搜索结果
   */
  fun searchEntriesByPackageName(
    pkgName: String,
    listStorage: MutableList<PwEntry>
  ) {
    for (entry in BaseApp.KDB.pm.entries.values) {
      val pe4 = entry as PwEntryV4
      if (pe4.strings == null || pe4.strings.isEmpty()) {
        continue
      }
      for (ps in pe4.strings.values) {
        if (ps.toString()
            .equals("androidapp://$pkgName", ignoreCase = true)
        ) {
          listStorage.add(pe4)
          break
        }
      }
    }
  }

  fun getGroupEntryNum(pwGroup: PwGroup): Int {
    return pwGroup.childEntries.size + pwGroup.childGroups.size
  }

  /**
   * 获取组中的条目数
   */
  fun getGroupAllEntryNum(pwGroup: PwGroup): Int {
    var num = 0
    if (pwGroup.childEntries.isEmpty() && pwGroup.childGroups.isEmpty()) {
      return 0
    }
    if (pwGroup.childGroups.isNotEmpty()) {
      for (group in pwGroup.childGroups) {
        num += getGroupAllEntryNum(group)
      }
      num += pwGroup.childEntries.size
    } else {
      num += pwGroup.childEntries.size
    }
    return num
  }

  fun findEntry(uuid: UUID): PwEntry? {
    val enties = BaseApp.KDB.pm.entries
    return enties[uuid]
  }

  /**
   * 通过id 获取v3的group信息
   */
  fun findV3GroupById(groupId: Int): PwGroup? {
    val groups = BaseApp.KDB.pm.groups
    return groups[PwGroupIdV3(groupId)]
  }

  fun getRootGroup():PwGroup{
    return BaseApp.KDB.pm.rootGroup
  }

  /**
   * 通过id 获取v4的group信息
   *
   * pm.groups 在某些场景下（如群组刚创建但 cache 未同步）可能查不到，
   * 此时回退到从 rootGroup 递归查找，确保不会因 cache 失血而丢组。
   */
  fun findV4GroupById(groupId: UUID): PwGroup? {
    val groups = BaseApp.KDB.pm.groups
    val key = PwGroupIdV4(groupId)
    val direct = groups[key]
    if (direct != null) {
      Timber.i(
        "findV4GroupById: pm.groups hit for %s, name=%s, uuid=%s, childEntries.size=%d",
        groupId,
        direct.name,
        (direct as? com.keepassdroid.database.PwGroupV4)?.uuid,
        direct.childEntries.size
      )
      return direct
    }

    Timber.w(
      "findV4GroupById: pm.groups miss for %s (groups.size=%d), falling back to tree walk",
      groupId,
      groups.size
    )
    val walked = findV4GroupByUuidRecursive(BaseApp.KDB.pm.rootGroup, groupId)
    if (walked == null) {
      Timber.e(
        "findV4GroupById: tree walk also failed for %s. Listing known groups:",
        groupId
      )
      groups.forEach { (id, g) ->
        Timber.e(
          "  - group id=%s, name=%s",
          (id as? com.keepassdroid.database.PwGroupIdV4)?.id,
          g.name
        )
      }
    } else {
      Timber.i(
        "findV4GroupById: tree walk found %s, uuid=%s, childEntries.size=%d",
        walked.name,
        (walked as? com.keepassdroid.database.PwGroupV4)?.uuid,
        walked.childEntries.size
      )
    }
    return walked
  }

  private fun findV4GroupByUuidRecursive(group: PwGroup, target: UUID): PwGroup? {
    if (group is com.keepassdroid.database.PwGroupV4 && group.uuid == target) return group
    for (child in group.childGroups) {
      findV4GroupByUuidRecursive(child, target)?.let { return it }
    }
    return null
  }

  fun filterCustomStr(map: Map<String, ProtectedString>): Map<String, ProtectedString> {
    val remap = HashMap<String, ProtectedString>()
    // var addOTPPass = false
    for (str in map) {
      if (str.key.equals(PwEntryV4.STR_NOTES, true)
        || str.key.equals(PwEntryV4.STR_PASSWORD, true)
        || str.key.equals(PwEntryV4.STR_TITLE, true)
        || str.key.equals(PwEntryV4.STR_URL, true)
        || str.key.equals(PwEntryV4.STR_USERNAME, true)
      ) {
        continue
      }

      remap[str.key] = str.value

      // // 增加TOP密码字段
      // if (!addOTPPass && (str.key.startsWith("TOTP", ignoreCase = true)
      //     || str.key.startsWith("OTP", ignoreCase = true))
      // ) {
      //   addOTPPass = true
      //   val totpPass = OtpUtil.getOtpPass(entryV4)
      //   if (TextUtils.isEmpty(totpPass.second)) {
      //     continue
      //   }
      //   val totpPassStr = ProtectedString(true, totpPass.second)
      //   totpPassStr.isOtpPass = true
      //   map["TOTP"] = totpPassStr
      // }
    }

    return remap.toList()
      .sortedBy { it.first }
      .toMap()
  }

  /**
   * 过滤并排序自定义字段和自定义数据
   */
  fun filterCustomStr(
    entryV4: PwEntryV4,
  ): Map<String, ProtectedString> {
    return filterCustomStr(entryV4.strings)
  }
}