package com.lyy.keepassa.view.dialog.webdav

import android.view.View
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.KeyboardUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogWebdavLoginNewBinding
import com.lyy.keepassa.util.HitUtil

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:25 上午 2022/7/21
 **/
internal class NextcloudLoginAdapter(
  val binding: DialogWebdavLoginNewBinding,
  val context: WebDavLoginDialogNew
) : IWebDavLoginAdapter {
  override fun updateState() {
    binding.groupHost.visibility = View.VISIBLE
    KeyboardUtils.showSoftInput(binding.uri)
  }

  override fun startLogin(userName: String, password: String) {
    val hostName = binding.host.text.toString().trim()
    if (hostName.isEmpty()) {
      HitUtil.toaskLong(ResUtil.getString(R.string.webdav_port_name_null))
      return
    }

    val url = context.module.convertHost(hostName, binding.edPort.text.toString().trim(), userName)
    context.startLoginFlow(url, userName, password)
  }
}