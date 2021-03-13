/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arialyy.frame.util.FileUtil
import com.lyy.keepassa.BuildConfig
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogWebdavLoginBinding
import com.lyy.keepassa.event.ChangeDbEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.getArgument
import com.lyy.keepassa.view.DbPathType.WEBDAV
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * webdav 登录
 */
class WebDavLoginDialog : BaseDialog<DialogWebdavLoginBinding>() {
  private var loadingDialog: LoadingDialog? = null
  private lateinit var module: WebDavLoginModule
  private var webDavUri: String? = null

  /**
   * true，创建数据库时的登录
   */
  private val webDavIsCreateLogin by lazy {
    getArgument<Boolean>("webDavIsCreateLogin") ?: false
  }

  fun getWebDavUri() = webDavUri

  override fun setLayoutId(): Int {
    return R.layout.dialog_webdav_login
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this).get(WebDavLoginModule::class.java)

    if (BuildConfig.DEBUG) {
      val p = FileUtil.loadConfig(File("${requireContext().filesDir.path}/webDav.properties"))
      binding.uri.setText(p.getProperty("uri"))
      binding.userName.setText(p.getProperty("userName"))
      binding.password.setText(p.getProperty("password"))
    }

    binding.enter.setOnClickListener {
      if (KeepassAUtil.instance.isFastClick()) {
        return@setOnClickListener
      }

      val pass = binding.password.text.toString()
          .trim()
      val uri = binding.uri.text.toString()
          .trim()
      val userName = binding.userName.text.toString()
          .trim()
      if (pass.isEmpty()) {
        HitUtil.toaskShort(getString(R.string.hint_please_input, getString(R.string.password)))
        return@setOnClickListener
      }
      if (userName.isEmpty()) {
        HitUtil.toaskShort(
            getString(R.string.hint_please_input, getString(R.string.hint_input_user_name))
        )
        return@setOnClickListener
      }
      if (uri.isEmpty() || uri.equals("null", true)) {
        HitUtil.toaskShort(
            getString(R.string.hint_please_input, getString(R.string.hint_webdav_url))
        )
        return@setOnClickListener
      }
      if (!KeepassAUtil.instance.checkUrlIsValid(uri)) {
        HitUtil.toaskShort("${getString(R.string.hint_webdav_url)} ${getString(R.string.invalid)}")
        return@setOnClickListener
      }

      val temp = Uri.parse(uri)

      if (webDavIsCreateLogin) {
        if (temp == null || !uri.endsWith("/", ignoreCase = true)) {
          HitUtil.toaskLong(getString(R.string.error_webdav_end_suffix))
          return@setOnClickListener
        }
      } else if (temp == null
          || TextUtils.isEmpty(temp.lastPathSegment)
          || !temp.lastPathSegment!!.endsWith(".kdbx", ignoreCase = true)
      ) {
        HitUtil.toaskLong(getString(R.string.hint_kdbx_name))
        return@setOnClickListener
      }

      // start login
      this.webDavUri = uri
      loadingDialog = LoadingDialog(context)
      loadingDialog?.show()
      module.checkLogin(requireContext(), uri, userName, pass, webDavIsCreateLogin)
          .observe(this, Observer { success ->
            loadingDialog?.dismiss()
            if (success) {
              var dbName = Uri.parse(uri).lastPathSegment
              if (dbName == null) {
                dbName = "unknown.kdbx"
              }
              if (!webDavIsCreateLogin) {
                EventBus.getDefault()
                    .post(
                        ChangeDbEvent(
                            dbName = dbName,
                            localFileUri = DbSynUtil.getCloudDbTempPath(WEBDAV.name, dbName),
                            cloudPath = uri,
                            uriType = WEBDAV
                        )
                    )
              }
              HitUtil.toaskShort("${getString(R.string.login)} ${getString(R.string.success)}")
              dismiss()
              return@Observer
            }

            HitUtil.toaskShort("${getString(R.string.login)} ${getString(R.string.fail)}")
          })
    }
    binding.cancel.setOnClickListener { dismiss() }
  }

}