/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arialyy.frame.base.BaseDialog
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.DialogAddGroupBinding
import com.lyy.keepassa.event.CreateOrUpdateGroupEvent
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.ChooseIconActivity
import com.lyy.keepassa.view.create.CreateEntryModule
import org.greenrobot.eventbus.EventBus

/**
 * 编辑群组
 */
class ModifyGroupDialog : BaseDialog<DialogAddGroupBinding>(), View.OnClickListener {

  private lateinit var loadDialog: LoadingDialog
  private var icon: PwIconStandard = PwIconStandard(1)
  private var customIcon: PwIconCustom? = null
  private val requestCode = 0xA1
  private lateinit var module: CreateEntryModule
  lateinit var modifyPwGroup: PwGroup

  companion object {
    fun generate(body: ModifyGroupDialog.() -> ModifyGroupDialog): ModifyGroupDialog {
      return with(ModifyGroupDialog()) { body() }
    }
  }

  fun build(): ModifyGroupDialog {
    return this
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_group
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this).get(CreateEntryModule::class.java)
    binding.groupNameLayout.setEndIconOnClickListener {
      startActivityForResult(
          Intent(context, ChooseIconActivity::class.java), requestCode,
          ActivityOptions.makeSceneTransitionAnimation(activity)
              .toBundle()
      )
    }
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
    binding.title.text = getString(R.string.modify_group)
    binding.groupName.setText(modifyPwGroup.name)
    binding.groupNameLayout.endIconDrawable =
      IconUtil.getGroupIconDrawable(requireContext(), modifyPwGroup, true)

    icon = modifyPwGroup.icon
    if (BaseApp.isV4) {
      customIcon = (modifyPwGroup as PwGroupV4).customIcon
    }
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.enter -> {
        val newTitle = binding.groupName.text.toString()
            .trim()
        if (newTitle.isEmpty()) {
          HitUtil.toaskShort(getString(R.string.error_group_name_null))
          return
        }

        loadDialog = LoadingDialog(context)
        loadDialog.show()
        if (BaseApp.isV4) {
          (modifyPwGroup as PwGroupV4).customIcon = customIcon
        }
        modifyPwGroup.icon = icon
        modifyPwGroup.name = newTitle
        module.saveDb()
            .observe(this, Observer { success ->
              loadDialog.dismiss()
              if (success) {
                complete(modifyPwGroup)
                return@Observer
              }
              HitUtil.toaskShort(getString(R.string.update_group_fail))
            })
      }
      R.id.cancel -> {
        dismiss()
      }
    }
  }

  private fun complete(group: PwGroup?) {
    if (group == null) {
      HitUtil.toaskShort(getString(R.string.update_group_fail))
    } else {
      HitUtil.toaskShort(getString(R.string.update_group_success))
      EventBus.getDefault()
          .post(CreateOrUpdateGroupEvent(group, true))
      dismiss()
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK && requestCode == this.requestCode && data != null) {
      val type =
        data.getIntExtra(ChooseIconActivity.KEY_ICON_TYPE, ChooseIconActivity.ICON_TYPE_STANDARD)
      if (type == ChooseIconActivity.ICON_TYPE_STANDARD) {
        icon = data.getSerializableExtra(ChooseIconActivity.KEY_DATA) as PwIconStandard
        binding.groupNameLayout.endIconDrawable =
          resources.getDrawable(IconUtil.getIconById(icon.iconId), requireContext().theme)
        customIcon = PwIconCustom.ZERO
        return
      }
      // 自定义图片
      customIcon = data.getSerializableExtra(ChooseIconActivity.KEY_DATA) as PwIconCustom
      binding.groupNameLayout.endIconDrawable =
        IconUtil.convertCustomIcon2Drawable(requireContext(), customIcon!!)
    }
  }

}