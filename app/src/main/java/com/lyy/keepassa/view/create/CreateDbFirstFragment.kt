/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.dropbox.core.android.Auth
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentCreateDbFirstBinding
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.AFS
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.UNKNOWN
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.MsgDialog.OnBtClickListener
import com.lyy.keepassa.view.dialog.WebDavLoginDialog
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * 创建数据库的第一步
 * 1、设置数据库保存类型
 * 2、设置数据库名
 */
class CreateDbFirstFragment : BaseFragment<FragmentCreateDbFirstBinding>() {
  private val PATH_REQUEST_CODE = 0xA1
  private lateinit var module: CreateDbModule
  private lateinit var pathTypeDialog: PathTypeDialog
  private var dropboxNeedAuth = false


  override fun initData() {
    module = ViewModelProvider(requireActivity()).get(CreateDbModule::class.java)
    initView()
    EventBusHelper.reg(this)
  }

  private fun initView() {
    setPathTypeInfo()
    showPathDialog()

    binding.pathType.setOnIconClickListener(object : OnIconClickListener {
      override fun onClick(
        view: BubbleTextView,
        index: Int
      ) {
        if (index == 2) {
          val dialog = MsgDialog.generate {
            msgTitle = this@CreateDbFirstFragment.getString(R.string.hint)
            msgContent = this@CreateDbFirstFragment.getString(R.string.help_create_db_path)
            showCancelBt = false
            build()
          }
          dialog.show()
        }
      }
    })

    // 设置键盘确定按钮属性
    binding.dbName.setOnEditorActionListener { _, actionId, _ ->
      if (!isAdded){
        return@setOnEditorActionListener false
      }
      // actionId 和android:imeOptions 属性要保持一致
      if (actionId == EditorInfo.IME_ACTION_DONE && !TextUtils.isEmpty(binding.dbName.text)) {
         KeepassAUtil.instance.toggleKeyBord(requireContext())
//        showPathDialog()
        startNext()
        true
      } else {
        false
      }
    }
  }

  /**
   * 处理数据库名没有设置的情况
   */
  fun handleDbNameNull() {
    val hint = getString(R.string.error_db_name_null)

    binding.dbNameLayout.error = hint
    binding.dbName.requestFocus()
    HitUtil.toaskShort(hint)
     KeepassAUtil.instance.toggleKeyBord(requireContext())
  }

  /**
   * 和其它fragment共享的元素
   */
  fun getShareElement(): View {
    return binding.dbName
  }

  /**
   * 获取数据库名
   */
  fun getDbName(): String {
    module.dbName = binding.dbName.text.toString()
        .trim()
    return module.dbName
  }

  fun showPathDialog() {
    pathTypeDialog = PathTypeDialog(
        binding.dbName.text.toString()
            .trim()
    )
    pathTypeDialog.showNow(childFragmentManager, "PathDialog")
    pathTypeDialog.setOnDismissListener {
      if (module.dbPathType == UNKNOWN) {
        requireActivity().finishAfterTransition()
        return@setOnDismissListener
      }
      setPathTypeInfo()
      // 处理类型选择
      when (module.dbPathType) {
        // webdav 启动登录
        WEBDAV -> {
//          changeWebDav()
        }
        // dropbox 检查授权
        DROPBOX -> {
          if (!DropboxUtil.isAuth()) {
            dropboxNeedAuth = true
            changeDropbox()
          }
        }
        // 其它的需要设置数据库名
        else -> {
          binding.dbName.requestFocus()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (dropboxNeedAuth) {
      val token = Auth.getOAuth2Token()
      if (!TextUtils.isEmpty(token)) {
        DropboxUtil.saveToken(token)
        HitUtil.toaskShort("dropbox ${getString(R.string.auth)}${getString(R.string.success)}")
      } else {
        HitUtil.toaskShort("dropbox ${getString(R.string.auth)}${getString(R.string.fail)}")
      }
    }
  }

  /**
   * 选择webdav路径，登录成功后直接进入下一个页面
   */
  private fun changeWebDav() {
    val uri = binding.dbName.text.toString()
        .trim()
    val webDavDialog = WebDavLoginDialog().apply {
      putArgument("webDavIsCreateLogin", true)
      putArgument("webDavDbName", uri)
    }
    webDavDialog.show(requireActivity().supportFragmentManager, "web_dav_login")
  }

  /**
   * 选择dropbox路径
   * 只有dropbox为授权才显示该对话框
   */
  private fun changeDropbox() {
    val msgDialog = MsgDialog.generate {
      msgTitle = this@CreateDbFirstFragment.getString(R.string.hint)
      msgContent = Html.fromHtml(this@CreateDbFirstFragment.getString(R.string.dropbox_msg))
      showCancelBt = false
      build()
    }
    msgDialog.setOnBtClickListener(object : OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        if (!isAdded){
          return
        }
        Auth.startOAuth2Authentication(requireContext(), DropboxUtil.APP_KEY)
      }
    })
    msgDialog.show()
  }

  /**
   * 获取数据库路径，将在这地方启动下一页面
   * webdav，dropbox的路径来自于eventBus
   */
  @Subscribe(threadMode = MAIN)
  fun onGetDbPath(pathEvent: DbPathEvent) {
    if (pathEvent.fileUri == null && pathEvent.dbPathType == AFS) {
      Log.e(TAG, "uri 获取失败")
      return
    }
    // 直接启动下一界面
    val startNextFragment = true

    when (pathEvent.dbPathType) {
      AFS -> {
        binding.dbNameLayout.visibility = View.VISIBLE
        module.dbUri = pathEvent.fileUri!!
        module.dbName = UriUtil.getFileNameFromUri(requireContext(), module.dbUri!!)
      }
      DROPBOX -> {
        binding.dbNameLayout.visibility = View.VISIBLE
        module.dbUri = DbSynUtil.getCloudDbTempPath(DROPBOX.name, getDbName())
        module.cloudPath = pathEvent.cloudDiskPath!!
        module.dbName = UriUtil.getFileNameFromUri(requireContext(), module.dbUri!!)
      }
      WEBDAV -> {
        binding.dbNameLayout.visibility = View.GONE
        val uri = Uri.parse(pathEvent.cloudDiskPath)
        module.dbName = uri.lastPathSegment ?: "unknown"
        module.dbUri = DbSynUtil.getCloudDbTempPath(WEBDAV.name, module.dbName)
        module.cloudPath = pathEvent.cloudDiskPath!!
//        startNextFragment = true
      }
    }


    binding.dbName.setText(module.dbName)
    setPathTypeInfo()

    if (startNextFragment) {
      (activity as CreateDbActivity).startNextFragment()
    }
  }

  /**
   * 检查是否可以进入下一步
   * 1、afs 需要设置数据库名，并且文件保存[CreateDbModule.dbUri]不为空
   * 2、dropbox需要校验通过，并且数据库名名不为空
   * 3、webdav需要校验通过，并且uri不为空
   */
  fun startNext(): Boolean {
    val temp = binding.dbName.text.toString()
        .trim()
    if (TextUtils.isEmpty(temp)) {
      HitUtil.toaskShort(getString(R.string.error_db_name_null))
      return false
    } else if (module.dbPathType == WEBDAV && !module.checkWebDavUri(requireContext(), temp)) {
      return false
    }

    when (module.dbPathType) {
      AFS -> {
        if (module.dbUri == null) {
           KeepassAUtil.instance.createFile(
              this, "*/*", "$temp.kdbx", PATH_REQUEST_CODE
          )
        }
      }
      DROPBOX -> {
        if (!DropboxUtil.isAuth()) {
          changeDropbox()
        } else {
          onGetDbPath(
              DbPathEvent(
                  dbName = "$temp.kdbx",
                  fileUri = DbSynUtil.getCloudDbTempPath(DROPBOX.name, temp),
                  dbPathType = DROPBOX,
                  cloudDiskPath = "/$temp.kdbx"
              )
          )
        }
      }
      WEBDAV -> {
//        changeWebDav()
      }
    }
    return true
  }

  /**
   * 设置文件路径类型提示
   */
  private fun setPathTypeInfo() {
    binding.pathType.text = module.dbPathType.lable
    binding.pathType.setLeftIcon(module.dbPathType.icon)
    setDbNameHint(module.dbPathType)
  }

  /**
   * 设置数据名输入提示
   */
  private fun setDbNameHint(dbPathType: DbPathType) {
    when (dbPathType) {
      WEBDAV -> {
        binding.dbNameLayout.helperText = getString(R.string.helper_webdav)
        binding.dbNameLayout.hint = getString(R.string.hint_webdav_url)
      }
      else -> {
        binding.dbNameLayout.helperText = getString(R.string.help_create_db)
        binding.dbNameLayout.hint = getString(R.string.db_name)
      }
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_create_db_first
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK
        && requestCode == PATH_REQUEST_CODE
        && data != null
        && data.data != null
        && context != null
    ) {
      if (!isAdded){
        return
      }
      // 申请长期的uri权限
      // 防止一个不可思议的空指针，data.data 有可能还是为空
      data.data?.apply {
        takePermission()
        onGetDbPath(
            DbPathEvent(
                dbName = UriUtil.getFileNameFromUri(requireContext(), this),
                fileUri = this,
                dbPathType = AFS
            )
        )
      }
    }
  }

}