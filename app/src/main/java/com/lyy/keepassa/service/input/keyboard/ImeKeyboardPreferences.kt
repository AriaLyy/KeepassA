package com.lyy.keepassa.service.input.keyboard

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.preference.PreferenceManager
import com.lyy.keepassa.R

internal class ImeKeyboardPreferences(
  private val context: Context
) {
  fun hapticFeedbackEnabled(): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(context.getString(R.string.set_key_ime_keyboard_haptic_feedback), true)

  fun performKeyboardHaptic(view: View) {
    if (!hapticFeedbackEnabled()) return
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
  }
}
