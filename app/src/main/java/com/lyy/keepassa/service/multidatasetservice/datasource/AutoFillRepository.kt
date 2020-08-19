/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.multidatasetservice.datasource

import android.content.Context
import com.lyy.keepassa.service.multidatasetservice.model.FilledAutoFillFieldCollection
import java.util.HashMap

interface AutoFillRepository {

  /**
   * Gets saved FilledAutofillFieldCollection that contains some objects that can autofill fields with these
   * `autofillHints`.
   */
  fun getFilledAutoFillFieldCollection(
    context: Context,
    pkgName: String,
    focusedAutoFillHints: List<String>,
    allAutoFillHints: List<String>
  ): HashMap<String, FilledAutoFillFieldCollection>?

  /**
   * Saves LoginCredential under this datasetName.
   */
  fun saveFilledAutoFillFieldCollection(
    context: Context,
    filledAutoFillFieldCollection: FilledAutoFillFieldCollection
  )

  /**
   * Clears all data.
   */
  fun clear(context: Context)
}