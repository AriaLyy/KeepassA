/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill.model

import android.util.Log
import android.view.autofill.AutofillId

/**
 * Data structure that stores a collection of `AutofillFieldMetadata`s. Contains all of the client's `View`
 * hierarchy autoFill-relevant metadata.
 */
data class AutoFillFieldMetadataCollection @JvmOverloads constructor(
  val autoFillIds: HashSet<AutofillId> = HashSet(),
  val allAutoFillHints: HashSet<String> = HashSet(),
  val focusedAutoFillHints: HashSet<String> = HashSet()
) {

  private val TAG = javaClass.simpleName

  /**
   * used for "other entry"
   */
  var tempUserFillId: AutofillId? = null
  var tempPassFillId: AutofillId? = null

  /**
   * key -> autoHintString
   * value ->
   */
  private val autoFillHintsToFieldsMap = HashMap<String, MutableList<AutoFillFieldMetadata>>()
  var saveType = 0
    private set

  fun clear() {
    tempUserFillId = null
    tempPassFillId = null
    autoFillIds.clear()
    allAutoFillHints.clear()
    focusedAutoFillHints.clear()
  }

  fun add(autoFillFieldMetadata: AutoFillFieldMetadata) {
    if (autoFillFieldMetadata.autoFillId == null) {
      Log.w(TAG, "autoFillId == null")
      return
    }

    saveType = saveType or autoFillFieldMetadata.saveType
    autoFillIds.add(autoFillFieldMetadata.autoFillId)
    val hintsList = autoFillFieldMetadata.autoFillHints
    allAutoFillHints.addAll(hintsList)
    if (autoFillFieldMetadata.isFocused) {
      focusedAutoFillHints.addAll(hintsList)
    }
    autoFillFieldMetadata.autoFillHints.forEach {
      val fields = autoFillHintsToFieldsMap[it] ?: ArrayList()
      autoFillHintsToFieldsMap[it] = fields
      fields.add(autoFillFieldMetadata)
    }
  }

  fun getFieldsForHint(hint: String): MutableList<AutoFillFieldMetadata>? {
    return autoFillHintsToFieldsMap[hint]
  }
}