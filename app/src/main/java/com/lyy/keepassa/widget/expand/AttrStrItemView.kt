/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.expand

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.widget.ProgressBar.RoundProgressBarWidthNumber

@SuppressLint("ViewConstructor")
class AttrStrItemView(
  context: Context,
  var titleStr: String,
  var valueInfo: ProtectedString
) : RelativeLayout(context) {
  private val TAG = javaClass.simpleName
  val titleTx: TextView
  val valueTx: TextView
  val pb: RoundProgressBarWidthNumber

  init {
    LayoutInflater.from(context).inflate(R.layout.layout_expand_child_str, this, true)
    titleTx = findViewById(R.id.title)
    valueTx = findViewById(R.id.value)
    pb = findViewById(R.id.rpbBar)
    updateValue(titleStr, valueInfo)
  }

  fun updateValue(
    key: String,
    valueInfo: ProtectedString
  ) {
    titleStr = key
    this.valueInfo = valueInfo
    titleTx.text = titleStr
    if (valueInfo.isProtected) {
      valueTx.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    } else {
      valueTx.inputType =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }
    valueTx.text = valueInfo.toString()
  }
}