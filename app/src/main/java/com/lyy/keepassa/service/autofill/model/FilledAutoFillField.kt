/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill.model

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.os.Build
import android.view.autofill.AutofillValue
import com.google.gson.annotations.Expose
import com.lyy.keepassa.service.autofill.AutoFillHelper

/**
 * JSON serializable data class containing the same data as an [AutofillValue].
 */
@TargetApi(Build.VERSION_CODES.O)
class FilledAutoFillField(viewNode: AssistStructure.ViewNode) {
  @Expose
  var textValue: String? = null

  @Expose
  var dateValue: Long? = null

  @Expose
  var toggleValue: Boolean? = null

  val autoFillHints = viewNode.autofillHints?.filter(AutoFillHelper::isValidHint)?.toTypedArray()

  init {
    viewNode.autofillValue?.let {
      when {
        it.isList -> {
          val index = it.listValue
          viewNode.autofillOptions?.let { autofillOptions ->
            if (autofillOptions.size > index) {
              textValue = autofillOptions[index].toString()
            }
          }
        }
        it.isDate -> {
          dateValue = it.dateValue
        }
        it.isText -> {
          // Using toString of AutofillValue.getTextValue in order to save it to
          // SharedPreferences.
          textValue = it.textValue.toString()
        }
        else -> {
        }
      }
    }
  }

  fun isNull(): Boolean {
    return textValue == null && dateValue == null && toggleValue == null
  }
}