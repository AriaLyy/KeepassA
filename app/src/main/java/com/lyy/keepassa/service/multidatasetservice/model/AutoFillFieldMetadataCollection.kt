/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lyy.keepassa.service.multidatasetservice.model

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
   * key -> autoHintString
   * value ->
   */
  private val autoFillHintsToFieldsMap = HashMap<String, MutableList<AutoFillFieldMetadata>>()
  var saveType = 0
    private set

  fun clear(){
    autoFillIds.clear()
    allAutoFillHints.clear()
    focusedAutoFillHints.clear()
  }

  fun add(autoFillFieldMetadata: AutoFillFieldMetadata) {
    if (autoFillFieldMetadata.autoFillId == null){
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
