package com.lyy.keepassa.util

import android.app.Service
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.lyy.keepassa.base.BaseApp

/**
 * 震动工具
 */
object VibratorUtil {
  private val vb: Vibrator = BaseApp.APP.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator

  /**
   * 震动一次
   */
  fun vibrator(time: Long) {
    if (!vb.hasVibrator()) {
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vb.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
      vb.vibrate(time)
    }
  }

  /**
   * 指定手机以pattern指定的模式振动
   * @param pattern pattern为new int[200,400,600,800],就是让他在200,400,600,800这个时间交替启动与关闭振动器!
   * @param repeat 而第二个则是重复次数,如果是-1的只振动一次,如果是0的话则一直振动
   */
  fun vibrate(
    pattern: LongArray,
    repeat: Int
  ) {
    if (!vb.hasVibrator()) {
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vb.vibrate(VibrationEffect.createWaveform(pattern, repeat))
    } else {
      vb.vibrate(pattern, repeat)
    }
  }

  fun cancel() {
    vb.cancel()
  }
}