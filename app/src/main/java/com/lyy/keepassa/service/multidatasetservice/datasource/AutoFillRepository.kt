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
