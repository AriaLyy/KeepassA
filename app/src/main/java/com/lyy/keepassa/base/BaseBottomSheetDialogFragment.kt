package com.lyy.keepassa.base

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.arialyy.frame.core.AbsBottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment<VB : ViewDataBinding> : AbsBottomSheetDialogFragment<VB>() {
  override fun init(savedInstanceState: Bundle?) {
  }
}