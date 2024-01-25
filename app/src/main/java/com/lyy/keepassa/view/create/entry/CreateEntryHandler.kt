/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.view.View
import androidx.core.view.isVisible
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwGroupV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:34 PM 2023/10/13
 **/
internal class CreateEntryHandler(val context: CreateEntryActivity) : ICreateHandler {

  override fun bindData() {
    val groupId =
      context.intent.getSerializableExtra(CreateEntryActivity.PARENT_GROUP_ID) as? PwGroupId
    val binding = context.binding
    val group =
      (if (groupId != null) BaseApp.KDB.pm.groups[groupId] else BaseApp.KDB.pm.rootGroup) as PwGroupV4
    val entry = PwEntryV4(group, true, true)
    context.module.pwEntry = entry
    binding.cardStr.visibility = View.GONE
    binding.cardFile.visibility = View.GONE
    binding.tlLoseTime.visibility = View.GONE
    binding.tlUrl.visibility = View.GONE
    binding.tlNote.visibility = View.GONE
    binding.tlTag.visibility = View.GONE
    binding.groupOtp.isVisible = false
  }

  override fun getTitle(): String {
    return ResUtil.getString(R.string.create_entry)
  }

  override fun saveDb(pwEntryV4: PwEntryV4) {
    checkAttr(context, pwEntryV4)
    context.launchGroupChoose()
  }
}