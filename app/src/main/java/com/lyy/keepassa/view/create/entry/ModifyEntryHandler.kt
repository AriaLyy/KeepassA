/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.loadImg
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:34 PM 2023/10/13
 **/
internal class ModifyEntryHandler(val context: CreateEntryActivity) : ICreateHandler {

  override fun bindData() {
    val entryId = context.intent.getSerializableExtra(CreateEntryActivity.KEY_ENTRY) as UUID
    context.module.pwEntry = BaseApp.KDB!!.pm.entries[entryId] as PwEntryV4
    val entry = context.module.pwEntry
    val binding = context.binding
    binding.title.setText(entry.title)
    binding.edUser.setText(entry.username)
    binding.edPassword.setText(entry.password)
    if (entry.notes.isNotEmpty()) {
      binding.tlNote.visibility = View.VISIBLE
      binding.edNote.setText(entry.notes.toString())
    }
    if (entry.url.isNotEmpty()) {
      binding.tlUrl.visibility = View.VISIBLE
      binding.edUrl.setText(entry.url)
    }
    handleIcon(context, entry)
    binding.cardStr.apply {
      visibility = if (entry.strings.isNotEmpty()) View.VISIBLE else View.GONE
      bindDate(entry)
    }
    binding.cardFile.apply {
      visibility = if (entry.binaries.isNotEmpty()) View.VISIBLE else View.GONE
      bindData(entry)
    }
    binding.tlTag.apply {
      visibility = if (entry.title.isNotEmpty()) View.VISIBLE else View.GONE
      binding.edTag.setText(entry.tags)
    }

    if (entry.expiryTime != null) {
      binding.edLoseTime.setText(DateTime(entry.expiryTime).toString(KdbUtil.DATE_FORMAT))
      binding.tlLoseTime.visibility = View.VISIBLE
    }
  }

  private fun handleIcon(ac: CreateEntryActivity, pwEntry: PwEntryV4) {
    ac.module.icon = pwEntry.icon
    ac.binding.ivIcon.loadImg(IconUtil.getEntryIconDrawable(ac, pwEntry, zoomIcon = true))
  }

  override fun getTitle(): String {
    return ResUtil.getString(R.string.edit)
  }

  override fun saveDb(pwEntryV4: PwEntryV4) {
    checkAttr(context, pwEntryV4)
    context.lifecycleScope.launch {
      KpaUtil.kdbHandlerService.saveOnly(true) {
        context.finishAfterTransition()
      }
    }
  }
}