/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud

import com.keepassdroid.database.PwDataInf

data class PwDataMap(
    val cloudPwData: PwDataInf,
    val localPwData: PwDataInf
  )