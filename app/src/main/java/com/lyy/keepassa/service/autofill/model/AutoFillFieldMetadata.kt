/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill.model

import android.annotation.TargetApi
import android.app.assist.AssistStructure.ViewNode;
import android.os.Build
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillId
import com.lyy.keepassa.service.autofill.AutoFillHelper

/**
 * A stripped down version of a [ViewNode] that contains only autofill-relevant metadata. It also
 * contains a `saveType` flag that is calculated based on the [ViewNode]'s autofill hints.
 */
@TargetApi(Build.VERSION_CODES.O)
class AutoFillFieldMetadata(viewNode: ViewNode) {

  var saveType = 0
    private set

  val autoFillHints = HashSet<String>()
  val autoFillId: AutofillId? = viewNode.autofillId
  val autoFillType: Int = viewNode.autofillType
  val autoFillOptions: Array<CharSequence>? = viewNode.autofillOptions
  val isFocused: Boolean = viewNode.isFocused
  var isPassword: Boolean = false
  val autoFillField = FilledAutoFillField(viewNode)

  /**
   * 处理自定义的情况，也就是控件没有设置android:autofillHints的情况
   * @param fileType view的类型 [View.AUTOFILL_HINT_PASSWORD]
   */
  constructor(
    view: ViewNode,
    fileType: String
  ) : this(view) {
    autoFillHints.add(fileType)
    updateSaveTypeFromHints()
  }

  /**
   * 处理控件中已经设置了android:autofillHints的情况
   */
  init {
    viewNode.autofillHints?.filter(AutoFillHelper::isValidHint)
        ?.forEach {
          autoFillHints.add(it)
        }
    updateSaveTypeFromHints()
  }

  /**
   * When the [ViewNode] is a list that the user needs to choose a string from (i.e. a spinner),
   * this is called to return the index of a specific item in the list.
   */
  fun getAutoFillOptionIndex(value: CharSequence): Int {
    return autoFillOptions?.indexOf(value) ?: -1
  }

  /**
   * 更新保存类型，和StructureParser.parseLocked 中的需要关联
   */
  private fun updateSaveTypeFromHints() {
    saveType = 0
    for (hint in autoFillHints) {
      when (hint) {
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY,
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
        View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
        View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> {
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD
        }
        View.AUTOFILL_HINT_EMAIL_ADDRESS -> {
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS
        }
        View.AUTOFILL_HINT_PHONE, View.AUTOFILL_HINT_NAME -> {
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_GENERIC
        }
        View.AUTOFILL_HINT_PASSWORD -> {
          isPassword = true
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
          saveType = saveType and SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS.inv()
          saveType = saveType and SaveInfo.SAVE_DATA_TYPE_USERNAME.inv()
        }
        View.AUTOFILL_HINT_POSTAL_ADDRESS,
        View.AUTOFILL_HINT_POSTAL_CODE -> {
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_ADDRESS
        }
        View.AUTOFILL_HINT_USERNAME -> {
          saveType = saveType or SaveInfo.SAVE_DATA_TYPE_USERNAME
        }
      }
    }
  }
}