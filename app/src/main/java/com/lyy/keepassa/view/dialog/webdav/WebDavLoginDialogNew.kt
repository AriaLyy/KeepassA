/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog.webdav

import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogWebdavLoginNewBinding
import com.lyy.keepassa.event.WebDavLoginEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.view.dialog.WebDavLoginModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * webdav 登录
 */
@Route(path = "/dialog/webdavLogin")
class WebDavLoginDialogNew : BaseDialog<DialogWebdavLoginNewBinding>() {

  lateinit var module: WebDavLoginModule
  val webDavLoginFlow = MutableSharedFlow<WebDavLoginEvent>()
  private var loginAdapter: IWebDavLoginAdapter? = null

  private val nextCloudAdapter by lazy {
    NextcloudLoginAdapter(binding, this)
  }

  private val defaultAdapter by lazy {
    DefaultLoginAdapter(binding, this)
  }

  private val otherAdapter by lazy {
    OtherLoginAdapter(binding, this)
  }

  private val loadingDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getLoadingDialog()
  }

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
    loginAdapter = defaultAdapter
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
      binding.groupHost.visibility = View.GONE
      module.curWebDavServer = it?.toString()
      loginAdapter = when {
        module.isNextcloud() -> {
          binding.isPreemptive.isChecked = true
          nextCloudAdapter
        }
        module.isJGY() -> {
          binding.isPreemptive.isChecked = false
          defaultAdapter
        }
        module.isOtherServer() -> otherAdapter
        else -> defaultAdapter
      }
      loginAdapter?.updateState()
    }
  }

  private fun handleEnterClick() {
    if (KeepassAUtil.instance.isFastClick()) {
      return
    }
    val pass = binding.password.text.toString()
      .trim()

    val userName = binding.userName.text.toString()
      .trim()
    if (pass.isEmpty()) {
      HitUtil.toaskLong(ResUtil.getString(R.string.hint_please_input, getString(R.string.password)))
      return
    }
    if (userName.isEmpty()) {
      HitUtil.toaskLong(
        ResUtil.getString(R.string.hint_please_input, getString(R.string.hint_input_user_name))
      )
      return
    }

    loginAdapter?.startLogin(userName, pass)
  }

  /**
   * 登陆流程
   */
  fun startLoginFlow(
    uri: String,
    userName: String,
    pass: String
  ) {
    loadingDialog.show()
    lifecycleScope.launch {
      module.checkLogin(uri, userName, pass, binding.isPreemptive.isChecked).collectLatest {
        loadingDialog.dismiss()
        webDavLoginFlow.emit(WebDavLoginEvent(uri, userName, pass, it))
        if (!it) {
          HitUtil.toaskLong("${getString(R.string.login)} ${getString(R.string.fail)}, url【${uri}】")
          return@collectLatest
        }
        HitUtil.toaskLong("${getString(R.string.login)} ${getString(R.string.success)}")
        dismiss()
      }
    }
  }
}