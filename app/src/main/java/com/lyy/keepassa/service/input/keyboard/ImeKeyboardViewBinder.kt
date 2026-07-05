package com.lyy.keepassa.service.input.keyboard

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.lyy.keepassa.R

internal class ImeKeyboardViewBinder(
  private val context: Context,
  private val container: LinearLayout,
  private val onKey: (View, ImeKeyAction) -> Unit,
  private val onShiftLongPress: (View) -> Unit
) {

  fun render(page: ImeKeyboardPage) {
    container.removeAllViews()
    val rows = when (page) {
      ImeKeyboardPage.ALPHABET -> ImeKeyboardLayout.alphabetRows()
      ImeKeyboardPage.SYMBOLS -> ImeKeyboardLayout.symbolRows()
    }
    rows.forEach { row ->
      container.addView(createRow(row))
    }
  }

  private fun createRow(actions: List<ImeKeyAction>): View {
    return LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      actions.forEach { action ->
        addView(createKey(action))
      }
    }
  }

  private fun createKey(action: ImeKeyAction): View {
    return TextView(context).apply {
      text = label(action)
      gravity = Gravity.CENTER
      minHeight = context.resources.getDimensionPixelSize(R.dimen.ime_key_height)
      setBackgroundResource(R.drawable.bg_ime_key)
      setTextColor(context.getColor(R.color.color_4E85DB))
      textSize = 16f
      isClickable = true
      isFocusable = true
      layoutParams = LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        weight(action)
      ).apply {
        setMargins(3, 3, 3, 3)
      }
      setOnClickListener { onKey(this, action) }
      if (action == ImeKeyAction.Shift) {
        setOnLongClickListener {
          onShiftLongPress(this)
          true
        }
      }
    }
  }

  private fun label(action: ImeKeyAction): String = when (action) {
    is ImeKeyAction.CommitText -> action.text
    ImeKeyAction.Space -> " "
    ImeKeyAction.Backspace -> "del"
    ImeKeyAction.Enter -> "enter"
    ImeKeyAction.Shift -> "shift"
    ImeKeyAction.SwitchToSymbols -> "123"
    ImeKeyAction.SwitchToAlphabet -> "ABC"
    ImeKeyAction.EnterSearchMode -> "search"
    ImeKeyAction.ClearSearch -> "clear"
    ImeKeyAction.ExitSearchMode -> "close"
  }

  private fun weight(action: ImeKeyAction): Float = when (action) {
    ImeKeyAction.Space -> 4f
    ImeKeyAction.Backspace,
    ImeKeyAction.Enter,
    ImeKeyAction.SwitchToSymbols,
    ImeKeyAction.SwitchToAlphabet,
    ImeKeyAction.Shift -> 1.5f
    else -> 1f
  }
}
