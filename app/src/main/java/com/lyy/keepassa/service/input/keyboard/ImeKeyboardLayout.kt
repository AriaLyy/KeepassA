package com.lyy.keepassa.service.input.keyboard

object ImeKeyboardLayout {

  fun alphabetRows(): List<List<ImeKeyAction>> = listOf(
    "qwertyuiop".toCommitTextActions(),
    "asdfghjkl".toCommitTextActions(),
    listOf(ImeKeyAction.Shift) + "zxcvbnm".toCommitTextActions() + ImeKeyAction.Backspace,
    listOf(
      ImeKeyAction.SwitchToSymbols,
      ImeKeyAction.CommitText(","),
      ImeKeyAction.Space,
      ImeKeyAction.CommitText("."),
      ImeKeyAction.Enter
    )
  )

  fun symbolRows(): List<List<ImeKeyAction>> = listOf(
    "1234567890".toCommitTextActions(),
    listOf("@", ".", "_", "-", "+", "/", ":", "?", "&").map { ImeKeyAction.CommitText(it) },
    listOf("=", "#", "%", "!", "*", "(", ")").map { ImeKeyAction.CommitText(it) },
    listOf(
      ImeKeyAction.SwitchToAlphabet,
      ImeKeyAction.Space,
      ImeKeyAction.Backspace,
      ImeKeyAction.Enter
    )
  )

  private fun String.toCommitTextActions(): List<ImeKeyAction.CommitText> =
    map { ImeKeyAction.CommitText(it.toString()) }
}
