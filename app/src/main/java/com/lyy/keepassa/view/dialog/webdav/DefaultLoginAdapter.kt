package com.lyy.keepassa.view.dialog.webdav

import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogWebdavLoginNewBinding
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:38 上午 2022/7/21
 **/
internal open class DefaultLoginAdapter(
  val binding: DialogWebdavLoginNewBinding,
  val context: WebDavLoginDialogNew
) : IWebDavLoginAdapter {
  override fun updateState() {
  }

  override fun startLogin(userName: String, password: String) {
    val uri = binding.uri.text.toString()
      .trim()

    if (uri.isEmpty() || uri.equals("null", true)) {
      HitUtil.toaskLong(
        ResUtil.getString(R.string.hint_please_input, ResUtil.getString(R.string.hint_webdav_url))
      )
      return
    }

    if (!KeepassAUtil.instance.checkUrlIsValid(uri)) {
      HitUtil.toaskLong("${ResUtil.getString(R.string.hint_webdav_url)} ${ResUtil.getString(R.string.invalid)}")
      return
    }

    context.startLoginFlow(uri, userName, password)
  }
}