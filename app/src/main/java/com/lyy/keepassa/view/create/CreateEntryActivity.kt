/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create

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
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.PwGroupId
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityEntryEditNewBinding
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.entity.TagBean
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.doClick
import com.lyy.keepassa.util.loadImg
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.create.CreateEnum.CREATE
import com.lyy.keepassa.view.create.CreateEnum.MODIFY
import com.lyy.keepassa.view.dialog.AddMoreDialog
import com.lyy.keepassa.view.dialog.ChooseTagDialog
import com.lyy.keepassa.view.dialog.CreateTagDialog
import com.lyy.keepassa.view.dialog.TimeChangeDialog
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback
import com.lyy.keepassa.view.launcher.LauncherActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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

  @Autowired(name = KEY_ENTRY)
  lateinit var entryId: UUID

  @Autowired(name = IS_SHORTCUTS)
  @JvmField
  var isShortcuts: Boolean = false

  @Autowired(name = KEY_TYPE)
  @JvmField
  var createEnum: CreateEnum = CREATE

  @Autowired(name = PARENT_GROUP_ID)
  @JvmField
  var groupId: PwGroupId? = null

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
      binding.edPassword.setText(it)
      binding.tvConfirm.setText(it)
    }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    ARouter.getInstance().inject(this)
    module = ViewModelProvider(this)[CreateEntryModule::class.java]
    module.pwEntry = BaseApp.KDB!!.pm.entries[entryId] as PwEntryV4
    createHandler = when (createEnum) {
      CREATE -> CreateEntryHandler(this)
      MODIFY -> ModifyEntryHandler(this)
    }
    createHandler.bindData()

    setTopBar()
    handlePassLayout()
    handleIconClick()
    handlerAddMore()
    listenerTag()
    listenerTimeChange()
  }

  /**
   * time change dialog
   */
  private fun listenerTimeChange() {
    lifecycleScope.launch {
      TimeChangeDialog.timeFlow.collectLatest { event ->
        if (event == null) {
          return@collectLatest
        }
        val time = "${event.year}/${event.month}/${event.dayOfMonth} ${event.hour}:${event.minute}"
        val dateTime = DateTime(
          event.year, event.month, event.dayOfMonth, event.hour, event.minute, DateTimeZone.UTC
        )
        module.loseDate = dateTime.toDate()
        binding.edLoseTime.setText(time)
      }
    }
  }

  private fun listenerTag() {
    lifecycleScope.launch {
      ChooseTagDialog.chooseTagFlow.collectLatest {
        val tags = it.joinToString(separator = ",")
        module.pwEntry.tags = tags
        binding.edTag.setText(tags)
      }
    }

    lifecycleScope.launch {
      CreateTagDialog.createTagFlow.collectLatest {
        Routerfit.create(DialogRouter::class.java)
          .showChooseTagDialog(module.pwEntry, if (it.isNullOrEmpty()) null else TagBean(it, true))
      }
    }
    module.getUserNameCache()
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
                Routerfit.create(DialogRouter::class.java).getTimeChangeDialog().show()
              }

            }
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
      if (module.hasTotp()) {
        addMoreData.remove(addMoreData.find { it.icon == R.drawable.ic_totp })
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
  }

  fun changeFile() {
    getFileLauncher.launch(arrayOf("*/*"))
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

  private fun handleIconClick() {
    binding.ivIcon.doClick {
      val iconDialog = IconBottomSheetDialog()
      iconDialog.setCallback(object : IconItemCallback {
        override fun onDefaultIcon(defIcon: PwIconStandard) {
          module.icon = defIcon
          binding.ivIcon.loadImg(ResUtil.getDrawable(IconUtil.getIconById(module.icon.iconId)))
          module.customIcon = PwIconCustom.ZERO
        }

        override fun onCustomIcon(customIcon: PwIconCustom) {
          module.customIcon = customIcon
          binding.ivIcon.loadImg(
            IconUtil.convertCustomIcon2Drawable(
              this@CreateEntryActivity,
              module.customIcon!!
            )
          )
        }
      })
      iconDialog.show(supportFragmentManager, IconBottomSheetDialog::class.java.simpleName)
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