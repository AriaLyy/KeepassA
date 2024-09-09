package com.lyy.keepassa.util

import androidx.core.view.animation.PathInterpolatorCompat

object InterpolatorConstance {
    val easeOutCubic = PathInterpolatorCompat.create(0.33f, 1f, 0.68f, 1f)
    val easeInCubic = PathInterpolatorCompat.create(0.32f, 0f, 0.67f, 0f)
}