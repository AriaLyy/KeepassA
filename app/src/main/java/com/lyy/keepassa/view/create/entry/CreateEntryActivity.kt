/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.entry

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.databinding.ActivityEntryEditNewBinding
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.entity.CommonState.DELETE
import com.lyy.keepassa.entity.GoogleOtpBean
import com.lyy.keepassa.entity.KeepassBean
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.entity.KpaIconType
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.entity.TagBean
import com.lyy.keepassa.entity.TrayTotpBean
import com.lyy.keepassa.entity.toOtpStringMap
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.hasTOTP
import com.lyy.keepassa.util.loadImg
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.util.totp.OtpEnum
import com.lyy.keepassa.view.create.CreateCustomStrDialog
import com.lyy.keepassa.view.create.GeneratePassActivity
import com.lyy.keepassa.view.create.entry.CreateEnum.CREATE
import com.lyy.keepassa.view.create.entry.CreateEnum.MODIFY
import com.lyy.keepassa.view.dialog.AddMoreDialog
import com.lyy.keepassa.view.dialog.ChooseTagDialog
import com.lyy.keepassa.view.dialog.CreateTagDialog
import com.lyy.keepassa.view.dialog.TimeChangeDialog
import com.lyy.keepassa.view.dialog.otp.CreateOtpModule
import com.lyy.keepassa.view.dir.ChooseGroupActivity
import com.lyy.keepassa.view.launcher.LauncherActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
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
    const val PARENT_GROUP_ID = "PARENT_GROUP_ID"

    /**
     * 数据库未解锁，保存数据时打开数据库，并保存
     */
    internal fun authAndSaveDb(
      context: Context,
      autoFillParam: AutoFillParam,
    ): IntentSender {
      val intent = Intent(context, CreateEntryActivity::class.java).also {
        it.putExtra(LauncherActivity.KEY_AUTO_FILL_PARAM, autoFillParam)
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      return PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
        .intentSender
    }
  }

  @Autowired(name = IS_SHORTCUTS)
  @JvmField
  var isShortcuts: Boolean = false

  @Autowired(name = KEY_TYPE)
  @JvmField
  var createEnum: CreateEnum = CREATE

  internal lateinit var module: CreateEntryModule
  private lateinit var createHandler: ICreateHandler
  private var isShowPass = false

  private var addMoreDialog: AddMoreDialog? = null
  private lateinit var addMoreData: ArrayList<SimpleItemEntity>

  private val getFileLauncher =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let {
        it.takePermission()
        module.addAttrFile(this, it)
      }
    }

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
      if (it.isEmpty()) {
        return@registerForActivityResult
      }
      binding.edPassword.setText(it)
      binding.tvConfirm.setText(it)
    }

  /**
   * 选择群组
   */
  private val chooseGroupLauncher =
    registerForActivityResult(object : ActivityResultContract<String?, PwGroupId?>() {
      override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, ChooseGroupActivity::class.java).apply {
          putExtra(ChooseGroupActivity.KEY_TYPE, ChooseGroupActivity.DATA_SELECT_GROUP)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): PwGroupId? {
        return intent?.getSerializableExtra(ChooseGroupActivity.DATA_PARENT) as PwGroupId?
      }
    }) {
      if (it == null) {
        Timber.d("pwGroupId is null")
        return@registerForActivityResult
      }
      module.updateEntryGroupIdAndSave(this, it)
    }

  fun launchGroupChoose() {
    chooseGroupLauncher.launch(null, ActivityOptionsCompat.makeSceneTransitionAnimation(this))
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[CreateEntryModule::class.java]

    createHandler = if (createEnum == MODIFY) {
      ModifyEntryHandler(this)
    } else {
      CreateEntryHandler(this)
    }

    createHandler.bindData()

    handleTopBarLayout()
    handlePassLayout()
    handleIconClick()
    handlerAddMore()
    handlerUserLayout()
    handleTimeLayout()
    handleTagLayout()
    handleTotpLayout()
    handleStr()
    handleAttrFile()
  }

  private fun handleAttrFile() {
    lifecycleScope.launch {
      CreateEntryModule.attrFlow.collectLatest { bean ->

        if (bean.state != DELETE) {
          module.fileCacheMap[bean.key] = bean.file
          binding.cardFile.isVisible = true
          binding.cardFile.bindData(module.fileCacheMap)
          return@collectLatest
        }

        module.fileCacheMap.remove(bean.key)

        binding.cardFile.removeItem(bean.key)
      }
    }
  }

  private fun handleStr() {
    lifecycleScope.launch {
      CreateCustomStrDialog.CustomStrFlow.collectLatest { bean ->
        if (bean == null) {
          Timber.d("attr is null")
          return@collectLatest
        }
        checkAddMoreBtn()
        if (bean.state != DELETE) {
          module.strCacheMap[bean.key] = bean.str
          binding.cardStr.isVisible = true
          binding.cardStr.bindDate(module.strCacheMap)
          return@collectLatest
        }

        module.strCacheMap.remove(bean.key)

        if (!module.strCacheMap.hasTOTP()) {
          binding.groupOtp.isVisible = false
        }

        binding.cardStr.removeItem(bean.key)
      }
    }
  }

  private fun handleTotpLayout() {
    fun startOtp() {
      binding.groupOtp.isVisible = true
      KdbUtil.startAutoGetOtp(module.pwEntry, binding.pbRound, binding.edOtp)
    }

    lifecycleScope.launch {
      CreateOtpModule.otpFlow.collectLatest {
        val map = when (it.first) {
          OtpEnum.TRAY_TOTP -> (it.second as? TrayTotpBean)?.toOtpStringMap()
          OtpEnum.KEEPASSXC -> (it.second as? KeepassXcBean)?.toOtpStringMap()
          OtpEnum.GOOGLE_OTP -> (it.second as? GoogleOtpBean)?.toOtpStringMap()
          OtpEnum.KEEPASS -> (it.second as? KeepassBean)?.toOtpStringMap()
        }
        map?.let { strs ->
          strs.forEach { kv ->
            module.strCacheMap[kv.key] = kv.value
          }
          startOtp()
          binding.cardStr.bindDate(module.strCacheMap)
        }
        checkAddMoreBtn()
      }
    }
    if (module.strCacheMap.hasTOTP()) {
      startOtp()
    }
    binding.edOtp.doClick {
      if (module.strCacheMap.hasTOTP()) {
        Routerfit.create(DialogRouter::class.java).showModifyOtpDialog(module.pwEntry.uuid)
        return@doClick
      }
      Routerfit.create(DialogRouter::class.java)
        .showCreateOtpDialog(module.pwEntry.title, module.pwEntry.username)
    }
  }

  private fun handlerUserLayout() {
    binding.edUser.threshold = 1 // 设置输入几个字符后开始出现提示 默认是2
    binding.edUser.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        binding.edUser.showDropDown()
      }
    }

    lifecycleScope.launch {
      CreateEntryModule.userNameFlow.collectLatest {
        if (it.isNullOrEmpty()) {
          return@collectLatest
        }
        binding.edUser.setAdapter(
          ArrayAdapter(
            this@CreateEntryActivity,
            R.layout.android_simple_dropdown_item_1line,
            it
          )
        )
      }
    }

    lifecycleScope.launch {
      module.getUserNameCache()
    }
  }

  private fun handleTimeLayout() {
    binding.edLoseTime.doClick {
      Routerfit.create(DialogRouter::class.java).showTimeChangeDialog()
    }

    lifecycleScope.launch {
      TimeChangeDialog.timeFlow.collectLatest { event ->
        if (event == null) {
          return@collectLatest
        }
        val time = "${event.year}/${event.month}/${event.dayOfMonth} ${event.hour}:${event.minute}"
        binding.edLoseTime.setText(time)
        binding.tlLoseTime.visibility = View.VISIBLE
        checkAddMoreBtn()
      }
    }
  }

  private fun handleTagLayout() {
    binding.edTag.doClick {
      Routerfit.create(DialogRouter::class.java).showChooseTagDialog(module.pwEntry)
    }

    lifecycleScope.launch {
      ChooseTagDialog.chooseTagFlow.collectLatest { tagBeanList ->
        val tagStrList = arrayListOf<String>()
        tagBeanList.forEach {
          tagStrList.add(it.tag)
        }
        val tags = tagStrList.joinToString(separator = ",")
        binding.edTag.setText(tags)
        binding.tlTag.visibility = View.VISIBLE
        checkAddMoreBtn()
      }
    }

    lifecycleScope.launch {
      CreateTagDialog.createTagFlow.collectLatest {
        Routerfit.create(DialogRouter::class.java)
          .showChooseTagDialog(module.pwEntry, if (it.isNullOrEmpty()) null else TagBean(it, true))
      }
    }
  }

  private fun handlerAddMore() {
    binding.btnAddMore.doClick {
      if (addMoreDialog == null) {
        addMoreData = module.getMoreItem(this)
        addMoreDialog = AddMoreDialog(addMoreData)
        addMoreDialog!!.setOnItemClickListener(object : AddMoreDialog.OnItemClickListener {
          override fun onItemClick(
            position: Int,
            item: SimpleItemEntity,
            view: View
          ) {
            when (item.icon) {
              R.drawable.ic_attr_str -> { // 自定义字段
                Routerfit.create(DialogRouter::class.java).showCreateCustomDialog()
              }

              R.drawable.ic_attr_file -> { // file
                changeFile()
              }

              R.drawable.ic_token_grey -> { // totp
                Routerfit.create(DialogRouter::class.java)
                  .showCreateOtpDialog(module.pwEntry.title, module.pwEntry.username)
              }

              R.drawable.ic_notice -> { // notice
                binding.tlNote.visibility = View.VISIBLE
                binding.tlNote.requestFocus()
              }

              R.drawable.ic_net -> { //url
                binding.tlUrl.visibility = View.VISIBLE
                binding.tlUrl.requestFocus()
              }

              R.drawable.ic_tag -> {
                Routerfit.create(DialogRouter::class.java).showChooseTagDialog(module.pwEntry)
              }

              R.drawable.ic_lose_time -> {
                Routerfit.create(DialogRouter::class.java).showTimeChangeDialog()
              }

            }
            checkAddMoreBtn()
            addMoreDialog!!.dismiss()
          }
        })
      }
      if (binding.tlLoseTime.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_lose_time })
      }
      if (binding.cardStr.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_attr_str })
      }
      if (binding.cardFile.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_attr_file })
      }
      if (module.pwEntry.hasTOTP()) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_token_grey })
      }
      if (binding.tlTag.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_tag })
      }
      if (binding.tlNote.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_notice })
      }
      if (binding.tlUrl.isVisible) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_net })
      }
      addMoreDialog!!.notifyData()
      addMoreDialog!!.show(supportFragmentManager, "add_more_dialog")
    }

    checkAddMoreBtn()
  }

  private fun checkAddMoreBtn() {
    if (binding.tlLoseTime.isVisible
      && binding.cardStr.isVisible
      && binding.cardFile.isVisible
      && module.pwEntry.hasTOTP()
      && binding.tlTag.isVisible
      && binding.tlNote.isVisible
      && binding.tlUrl.isVisible
    ) {
      binding.btnAddMore.visibility = View.GONE
    } else {
      binding.btnAddMore.visibility = View.VISIBLE
    }
  }

  fun changeFile() {
    getFileLauncher.launch(arrayOf("*/*"))
  }

  /**
   * 标题栏
   */
  private fun handleTopBarLayout() {
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
          createHandler.saveDb(module.pwEntry)
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

  private fun handleIconClick() {
    fun showIconChangeDialog() {
      SelectIconDialog().show()
    }
    binding.ivIcon.doClick {
      showIconChangeDialog()
    }

    binding.tvEdit.doClick {
      showIconChangeDialog()
    }

    lifecycleScope.launch {
      SelectIconDialog.iconResultFlow.collectLatest {
        if (it.first == KpaIconType.DEFAULT) {
          module.icon = it.second as PwIconStandard
          module.customIcon = PwIconCustom.ZERO
          binding.ivIcon.loadImg(ResUtil.getDrawable(IconUtil.getIconById(module.icon.iconId)))
          return@collectLatest
        }

        if (it.first == KpaIconType.CUSTOM) {
          module.customIcon = it.second as PwIconCustom
          binding.ivIcon.loadImg(
            IconUtil.convertCustomIcon2Drawable(
              this@CreateEntryActivity,
              module.customIcon!!
            )
          )
        }

        Timber.e("not support type: ${it.first}")
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
        binding.edPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      } else {
        binding.tlPass.endIconDrawable =
          ResUtil.getDrawable(R.drawable.ic_view_off)
        binding.tlConfirm.visibility = View.VISIBLE
        binding.edPassword.inputType =
          InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
      }
      // 将光标移动到最后
      binding.edPassword.setSelection(binding.edPassword.text?.length ?: 0)
      binding.edPassword.requestFocus()
    }
    binding.ivGeneratePw.setOnClickListener {
      passGenerateLauncher.launch(null, ActivityOptionsCompat.makeSceneTransitionAnimation(this))
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_entry_edit_new
  }
}