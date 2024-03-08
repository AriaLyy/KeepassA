/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.text.Html
import androidx.core.view.isVisible
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ToastUtils
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.util.KdbUtil
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:32 PM 2023/10/13
 **/
interface ICreateHandler {

  companion object {
    private val PROTECTED_KEY = arrayListOf<String>().apply {
      add(PwEntryV4.STR_TITLE)
      add(PwEntryV4.STR_USERNAME)
      add(PwEntryV4.STR_PASSWORD)
      add(PwEntryV4.STR_URL)
      add(PwEntryV4.STR_NOTES)
    }
  }

  fun bindData()

  fun getTitle(): String

  fun saveDb(pwEntryV4: PwEntryV4)

  fun checkAttr(context: CreateEntryActivity, pwEntryV4: PwEntryV4) {
    val binding = context.binding
    val title = binding.title.text.toString()
    val db = BaseApp.KDB.pm
    pwEntryV4.setTitle(title.ifEmpty { "unknown" }, db)
    pwEntryV4.setUsername(binding.edUser.text.toString(), db)
    pwEntryV4.setPassword(binding.edPassword.text.toString(), db)
    pwEntryV4.setUrl(binding.edUrl.text.toString(), db)
    pwEntryV4.tags = binding.edTag.text.toString()

    val loseTime = binding.edLoseTime.text.toString()
    if (loseTime.isNotEmpty() && binding.tlLoseTime.isVisible) {
      pwEntryV4.expiryTime =
        DateTime.parse(loseTime, DateTimeFormat.forPattern(KdbUtil.DATE_FORMAT)).toDate()
    }
    if (binding.tlNote.isVisible) {
      pwEntryV4.setNotes(binding.edNote.text.toString(), db)
    }

    if (binding.cardStr.isVisible) {
      val temp = arrayListOf<String>()
      pwEntryV4.strings.forEach {
        if (it.key in PROTECTED_KEY) {
          return@forEach
        }
        temp.add(it.key)
      }
      temp.forEach {
        pwEntryV4.strings.remove(it)
      }

      context.module.strCacheMap.filter { it.key != KeyConstance.TOTP && it.key !in PROTECTED_KEY }
        .forEach {
          pwEntryV4.strings[it.key] = it.value
        }
    }

    if (binding.cardFile.isVisible && checkEntry(pwEntryV4)) {
      pwEntryV4.binaries.clear()
      context.module.fileCacheMap.forEach {
        pwEntryV4.binaries[it.key] = it.value
      }
    }
    pwEntryV4.customIcon = context.module.customIcon
    pwEntryV4.icon = context.module.icon
  }

  private fun checkEntry(pwEntry: PwEntryV4): Boolean {
    pwEntry.binaries.forEach {
      if (it.value == null || it.value.length() == 0) {
        ToastUtils.showLong(Html.fromHtml(ResUtil.getString(R.string.error_file, it.key)))
        return false
      }
    }
    return true
  }
}