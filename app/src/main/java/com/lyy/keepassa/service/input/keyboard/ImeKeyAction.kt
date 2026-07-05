package com.lyy.keepassa.service.input.keyboard

sealed class ImeKeyAction {
  data class CommitText(val text: String) : ImeKeyAction()
  object Space : ImeKeyAction()
  object Backspace : ImeKeyAction()
  object Enter : ImeKeyAction()
  object Shift : ImeKeyAction()
  object SwitchToSymbols : ImeKeyAction()
  object SwitchToAlphabet : ImeKeyAction()
  object EnterSearchMode : ImeKeyAction()
  object ClearSearch : ImeKeyAction()
  object ExitSearchMode : ImeKeyAction()
}
