/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.KeyboardUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogWebdavLoginNewBinding
import com.lyy.keepassa.event.WebDavLoginEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * webdav 登录
 */
@Route(path = "/dialog/webdavLogin")
class WebDavLoginDialogNew : BaseDialog<DialogWebdavLoginNewBinding>() {

  private lateinit var module: WebDavLoginModule
  private var webDavUri: String? = null
  val webDavLoginFlow = MutableSharedFlow<WebDavLoginEvent>()

  override fun setLayoutId(): Int {
    return R.layout.dialog_webdav_login_new
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this)[WebDavLoginModule::class.java]
    binding.enter.setOnClickListener {
      handleEnterClick()

    }
    binding.cancel.setOnClickListener { dismiss() }
    handleSelectService()
  }

  private fun handleSelectService() {
    val adapter = ArrayAdapter(
      requireContext(),
      R.layout.android_simple_dropdown_item_1line,
      WebDavUtil.SUPPORTED_WEBDAV_URLS
    )
    binding.uri.setAdapter(adapter)
    binding.uri.threshold = 10000 // 设置输入几个字符后开始出现提示 默认是2
    binding.uri.setText(WebDavUtil.SUPPORTED_WEBDAV_URLS[0])
    binding.uriLayout.setEndIconOnClickListener {
      binding.uri.showDropDown()
    }
    binding.uri.doAfterTextChanged {
      if (it?.toString() == WebDavUtil.SUPPORTED_WEBDAV_URLS[WebDavUtil.SUPPORTED_WEBDAV_URLS.size - 1]) {
        binding.uri.setText("")
        KeyboardUtils.showSoftInput(binding.uri)
      }
    }
  }

  private fun handleEnterClick() {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    binding.userName.setText("511455842@qq.com")
    binding.password.setText("aux8q3tnmg9nqnq7")

    val pass = binding.password.text.toString()
      .trim()
    val uri = binding.uri.text.toString()
      .trim()
    val userName = binding.userName.text.toString()
      .trim()
    if (pass.isEmpty()) {
      HitUtil.toaskLong(getString(R.string.hint_please_input, getString(R.string.password)))
      return
    }
    if (userName.isEmpty()) {
      HitUtil.toaskLong(
        getString(R.string.hint_please_input, getString(R.string.hint_input_user_name))
      )
      return
    }
    if (uri.isEmpty() || uri.equals("null", true)) {
      HitUtil.toaskLong(
        getString(R.string.hint_please_input, getString(R.string.hint_webdav_url))
      )
      return
    }
    if (!KeepassAUtil.instance.checkUrlIsValid(uri)) {
      HitUtil.toaskLong("${getString(R.string.hint_webdav_url)} ${getString(R.string.invalid)}")
      return
    }

    // start login
    this.webDavUri = uri

    handleLoginFlow(uri, userName, pass)
  }

  /**
   * 登陆流程
   */
  private fun handleLoginFlow(
    uri: String,
    userName: String,
    pass: String
  ) {
    lifecycleScope.launch {
      module.checkLogin(uri, userName, pass).collectLatest {
        webDavLoginFlow.emit(WebDavLoginEvent(uri, userName, pass, it))
        if (!it) {
          HitUtil.toaskLong("${getString(R.string.login)} ${getString(R.string.fail)}")
          return@collectLatest
        }
        HitUtil.toaskLong("${getString(R.string.login)} ${getString(R.string.success)}")
        dismiss()
      }
    }
  }
}