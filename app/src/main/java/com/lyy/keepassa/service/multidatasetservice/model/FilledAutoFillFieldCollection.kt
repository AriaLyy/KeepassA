/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.multidatasetservice.model

import android.annotation.TargetApi
import android.os.Build
import android.service.autofill.Dataset
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.google.gson.annotations.Expose
import com.lyy.keepassa.util.KLog
import java.util.HashMap

/**
 * FilledAutofillFieldCollection is the model that represents all of the form data on a client app's page, plus the
 * dataset name associated with it.
 */
@TargetApi(Build.VERSION_CODES.O)
class FilledAutoFillFieldCollection @JvmOverloads constructor(
  @Expose var datasetName: String? = null,
  @Expose private val hintMap: HashMap<String, FilledAutoFillField> = HashMap()
) {

  private val TAG = javaClass.simpleName
  /**
   * Sets values for a list of autofillHints.
   */
  fun add(autofillField: FilledAutoFillField) {
    autofillField.autoFillHints?.forEach { autofillHint ->
      hintMap[autofillHint] = autofillField
    }
  }

  /**
   * Populates a [Dataset.Builder] with appropriate values for each [AutofillId]
   * in a `AutofillFieldMetadataCollection`. In other words, it builds an Autofill dataset
   * by applying saved values (from this `FilledAutofillFieldCollection`) to Views specified
   * in a `AutofillFieldMetadataCollection`, which represents the current page the user is
   * on.
   */
  fun applyToFields(
    autofillFieldMetadataCollection: AutoFillFieldMetadataCollection,
    datasetBuilder: Dataset.Builder
  ): Boolean {
    var setValueAtLeastOnce = false
    for (hint in autofillFieldMetadataCollection.allAutoFillHints) {
      val autofillFields = autofillFieldMetadataCollection.getFieldsForHint(hint) ?: continue
      for (autofillField in autofillFields) {
        val autofillId = autofillField.autoFillId
        if (autofillId == null){
          KLog.e(TAG, "autofillId == null")
          break
        }

        val autofillType = autofillField.autoFillType
        val savedAutofillValue = hintMap[hint]
        when (autofillType) {
          View.AUTOFILL_TYPE_LIST -> {
            savedAutofillValue?.textValue?.let {
              val index = autofillField.getAutoFillOptionIndex(it)
              if (index != -1) {
                datasetBuilder.setValue(autofillId, AutofillValue.forList(index))
                setValueAtLeastOnce = true
              }
            }
          }
          View.AUTOFILL_TYPE_DATE -> {
            savedAutofillValue?.dateValue?.let { date ->
              datasetBuilder.setValue(autofillId, AutofillValue.forDate(date))
              setValueAtLeastOnce = true
            }
          }
          View.AUTOFILL_TYPE_TEXT -> {
            savedAutofillValue?.textValue?.let { text ->
              datasetBuilder.setValue(autofillId, AutofillValue.forText(text))
              setValueAtLeastOnce = true
            }
          }
          View.AUTOFILL_TYPE_TOGGLE -> {
            savedAutofillValue?.toggleValue?.let { toggle ->
              datasetBuilder.setValue(autofillId, AutofillValue.forToggle(toggle))
              setValueAtLeastOnce = true
            }
          }
          else -> Log.w(TAG, "Invalid autofill type - " + autofillType)
        }
      }
    }
    return setValueAtLeastOnce
  }

  /**
   * @param autofillHints List of autofill hints, usually associated with a View or set of Views.
   * @return whether any of the filled fields on the page have at least 1 autofillHint that is
   * in the provided autofillHints.
   */
  fun helpsWithHints(autofillHints: List<String>): Boolean {
    for (autofillHint in autofillHints) {
      hintMap[autofillHint]?.let { savedAutofillValue ->
        if (!savedAutofillValue.isNull()) {
          return true
        }
      }
    }
    return false
  }
}