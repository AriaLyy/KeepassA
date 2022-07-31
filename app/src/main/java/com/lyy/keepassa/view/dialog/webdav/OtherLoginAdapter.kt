package com.lyy.keepassa.view.dialog.webdav

import com.blankj.utilcode.util.KeyboardUtils
import com.lyy.keepassa.databinding.DialogWebdavLoginNewBinding

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:46 上午 2022/7/21
 **/
internal class OtherLoginAdapter(
  binding: DialogWebdavLoginNewBinding,
  context: WebDavLoginDialogNew
) : DefaultLoginAdapter(binding, context) {
  override fun updateState() {
    binding.uri.setText("")
    KeyboardUtils.showSoftInput(binding.uri)
  }
}
