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