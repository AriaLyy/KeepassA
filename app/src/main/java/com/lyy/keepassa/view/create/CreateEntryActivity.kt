/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityEntryEditNewBinding
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.create.CreateEnum.CREATE
import com.lyy.keepassa.view.create.CreateEnum.MODIFY
import java.util.UUID
import kotlin.math.abs

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:24 PM 2023/10/13
 **/
@Route(path = "/entry/create")
class CreateEntryActivity : BaseActivity<ActivityEntryEditNewBinding>() {
  companion object {
    const val KEY_ENTRY = "KEY_ENTRY"

    /**
     * 类型，1：新建条目，2：利用模版新建条目，3：编辑条目
     */
    const val KEY_TYPE = "KEY_IS_TYPE"
    const val IS_SHORTCUTS = "isShortcuts"
  }

  @Autowired(name = KEY_ENTRY)
  lateinit var entryId: UUID

  @Autowired(name = IS_SHORTCUTS)
  @JvmField
  var isShortcuts: Boolean = false

  @Autowired(name = KEY_TYPE)
  @JvmField
  var createEnum: CreateEnum = CREATE

  internal lateinit var module: CreateEntryModule
  private lateinit var createHandler: ICreateHandler
  private var isShowPass = false

  /**
   * 密码创建器
   */
  private val passGenerateLauncher =
    registerForActivityResult(object : ActivityResultContract<String?, String>() {
      override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, GeneratePassActivity::class.java)
      }

      override fun parseResult(resultCode: Int, intent: Intent?): String {
        return intent?.getStringExtra(GeneratePassActivity.DATA_PASS_WORD) ?: ""
      }
    }) {
      binding.tvPassword.setText(it)
      binding.tvConfirm.setText(it)
    }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[CreateEntryModule::class.java]
    createHandler = when (createEnum) {
      CREATE -> CreateEntryHandler()
      MODIFY -> ModifyEntryHandler()
    }
    createHandler.initData(this)

    setTopBar()
    handlePassLayout()
  }

  /**
   * 标题栏
   */
  private fun setTopBar() {
    binding.topAppBar.title = createHandler.getTitle()
    toolbar = binding.topAppBar
    toolbar.setNavigationOnClickListener {
      finishAfterTransition()
    }
    toolbar.inflateMenu(R.menu.menu_entry_edit)

    toolbar.setOnMenuItemClickListener { item ->
      if (KeepassAUtil.instance.isFastClick()) {
        return@setOnMenuItemClickListener true
      }
      when (item.itemId) {
        R.id.save -> {
          TODO("Not yet implemented")
        }

        R.id.cancel -> {
          finishAfterTransition()
        }
      }

      true
    }
    binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
      if (verticalOffset == 0) {
        binding.topAppBar.title = ""
        return@addOnOffsetChangedListener
      }
      if (abs(verticalOffset) >= binding.appBarLayout.totalScrollRange) {
        binding.topAppBar.title = createHandler.getTitle()
        return@addOnOffsetChangedListener
      }
    }
  }

  /**
   * 处理密码
   */
  private fun handlePassLayout() {
    binding.tlPass.endIconDrawable = ResUtil.getDrawable(R.drawable.ic_view_off)

    binding.tlPass.setEndIconOnClickListener {
      isShowPass = !isShowPass
      if (isShowPass) {
        binding.tlPass.endIconDrawable = ResUtil.getDrawable(R.drawable.ic_view)
        binding.tlConfirm.visibility = View.GONE
        binding.tvPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      } else {
        binding.tlPass.endIconDrawable =
          ResUtil.getDrawable(R.drawable.ic_view_off)
        binding.tlConfirm.visibility = View.VISIBLE
        binding.tvPassword.inputType =
          InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
      }
      // 将光标移动到最后
      binding.tvPassword.setSelection(binding.tvPassword.text?.length ?: 0)
      binding.tvPassword.requestFocus()
    }
    binding.ivGeneratePw.setOnClickListener {
      passGenerateLauncher.launch(null, ActivityOptionsCompat.makeSceneTransitionAnimation(this))
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_detail_new
  }
}