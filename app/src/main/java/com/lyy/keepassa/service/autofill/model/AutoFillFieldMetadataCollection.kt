/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.autofill.model

import android.view.autofill.AutofillId
import timber.log.Timber

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
      Timber.w("autoFillId == null")
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

  /**
   * 从指定 hint 的字段列表中移除某个 [autoFillId] 对应的字段。若该 id 不再被任何 hint
   * 引用,会同时从 [autoFillIds] 中移除。用于多 TOTP 候选歧义消解等"事后修剪"场景。
   */
  fun removeField(autoFillId: AutofillId, hint: String) {
    val list = autoFillHintsToFieldsMap[hint] ?: return
    val iterator = list.iterator()
    while (iterator.hasNext()) {
      if (iterator.next().autoFillId == autoFillId) {
        iterator.remove()
        break
      }
    }
    if (list.isEmpty()) {
      autoFillHintsToFieldsMap.remove(hint)
    }
    val stillReferenced = autoFillHintsToFieldsMap.values.any { fields ->
      fields.any { it.autoFillId == autoFillId }
    }
    if (!stillReferenced) {
      autoFillIds.remove(autoFillId)
    }
  }
}