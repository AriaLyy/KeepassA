/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.arialyy.frame.base.net.INetResponse
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityOneDriveBinding
import com.lyy.demo.onedrive.MSAL
import com.lyy.demo.onedrive.MsalSourceItem
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/5
 * api 接口：https://docs.azure.cn/zh-cn/active-directory/develop/microsoft-graph-intro
 **/
class OneDriveActivity : AbsActivity<ActivityOneDriveBinding>() {
  private lateinit var oneDriveApp: ISingleAccountPublicClientApplication
  private var authInfo: IAuthenticationResult? = null

  override fun setLayoutId(): Int {
    return R.layout.activity_one_drive
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    initOneDrive()
  }

  private fun initOneDrive() {
    PublicClientApplication.createSingleAccountPublicClientApplication(this,
        R.raw.auth_config_single_account,
        object : ISingleAccountApplicationCreatedListener {
          override fun onCreated(application: ISingleAccountPublicClientApplication) {
            /**
             * This test app assumes that the app is only going to support one account.
             * This requires "account_mode" : "SINGLE" in the config json file.
             */
            oneDriveApp = application
            Log.d(TAG, "初始化成功")
          }

          override fun onError(exception: MsalException) {
            exception.printStackTrace()
          }
        })
  }

  /**
   * 加载用户，没有登陆过，则需要重新登陆
   */
  private fun loadAccount() {
    if (!this::oneDriveApp.isInitialized) {
      Log.e(TAG, "还没有初始化sdk")
      return
    }

    oneDriveApp.getCurrentAccountAsync(object : CurrentAccountCallback {
      override fun onAccountLoaded(activeAccount: IAccount?) {
        if (activeAccount == null) {
          Log.w(TAG, "用户还没有登陆")
          login()
        } else {
          Log.w(TAG, "已经登陆过，自动登陆，开始获取token")
          getTokenByAccountInfo(activeAccount)
        }
      }

      override fun onAccountChanged(
        priorAccount: IAccount?,
        currentAccount: IAccount?
      ) {
        Log.w(TAG, "账号已却换，重新获取token")
        if (currentAccount == null){
          Log.e(TAG, "当前账户为空")
          return
        }
        getTokenByAccountInfo(currentAccount)
      }

      override fun onError(exception: MsalException) {
        exception.printStackTrace()
      }
    })

  }

  /**
   * 根据用户信息获取token
   */
  private fun getTokenByAccountInfo(account: IAccount) {
    oneDriveApp.acquireTokenSilentAsync(getScopes(), account.authority, object : SilentAuthenticationCallback {
      override fun onSuccess(authenticationResult: IAuthenticationResult?) {
        Log.d(TAG, "获取token成功")
        authInfo = authenticationResult
        MSAL.updateAuthInfo(authInfo)
      }

      override fun onError(exception: MsalException) {
        exception.printStackTrace()
        Toast.makeText(this@OneDriveActivity, "获取token失败", Toast.LENGTH_SHORT).show()
      }

    })
  }

  private fun login() {
    oneDriveApp.signIn(this, "", getScopes(), object : AuthenticationCallback {
      override fun onSuccess(authenticationResult: IAuthenticationResult?) {
        authInfo = authenticationResult
        MSAL.updateAuthInfo(authInfo)
        Toast.makeText(this@OneDriveActivity, "登陆成功", Toast.LENGTH_SHORT)
            .show()
        Log.d(TAG, "登陆成功")
      }

      override fun onError(exception: MsalException?) {
        Toast.makeText(this@OneDriveActivity, "登陆失败", Toast.LENGTH_SHORT)
            .show()
        Log.d(TAG, "登陆失败")
        exception?.printStackTrace()
      }

      override fun onCancel() {
        Log.d(TAG, "取消登陆")
        Toast.makeText(this@OneDriveActivity, "登陆取消", Toast.LENGTH_SHORT)
            .show()
      }

    })
  }

  private fun getScopes(): Array<String> {
    return arrayOf("User.Read", "Files.ReadWrite.AppFolder")
  }

  private fun getAppDirList() {
    MSAL.getAppDir(authInfo?.account?.id ?: "", object : INetResponse<List<MsalSourceItem>> {
      override fun onResponse(response: List<MsalSourceItem>?) {

      }

      override fun onFailure(e: Throwable?) {
        e?.printStackTrace()
      }

    })
  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.btLogin -> {
        loadAccount()
      }
      R.id.fileList -> {
        if (!this::oneDriveApp.isInitialized) {
          Log.d(TAG, "没有初始化")
          return
        }
        getAppDirList()
      }
      R.id.upload -> {
        if (!this::oneDriveApp.isInitialized) {
          Log.d(TAG, "没有初始化")
        }

      }
      R.id.download -> {
        if (!this::oneDriveApp.isInitialized) {
          Log.d(TAG, "没有初始化")
        }
      }
    }
  }
}