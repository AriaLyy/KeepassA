/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:32 PM 2023/10/13
 **/
interface ICreateHandler {

  fun registerCustomListener(context: CreateEntryActivity) {
    context.lifecycleScope.launch {
      CreateCustomStrDialog.createCustomStrFlow.collectLatest {
        if (it == null) {
          Timber.w("create str fail, data is null")
          return@collectLatest
        }
        val cardStr = context.binding.cardStr
        val pwEntry = context.module.pwEntry
        if (!cardStr.isVisible) {
          cardStr.visibility = View.VISIBLE
        }
        pwEntry.strings[it.key] = it.str
        cardStr.bindDate(pwEntry)
      }
    }
  }

  fun bindData()

  fun getTitle(): String
}