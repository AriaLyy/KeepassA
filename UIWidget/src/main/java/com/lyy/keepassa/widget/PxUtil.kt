/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget

import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * [Int] db -> px
 */
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).roundToInt()

/**
 * [Int] sp -> px
 */
fun Int.toSp(): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP,
    this.toFloat(), Resources.getSystem().displayMetrics
).roundToInt()