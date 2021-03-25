/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.keepassdroid.database.PwGroup
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogAddGroupBinding
import com.lyy.keepassa.event.CreateOrUpdateGroupEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.dialog.LoadingDialog
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback
import org.greenrobot.eventbus.EventBus

/**
 * 创建或编辑群组dialog
 */
class CreateGroupDialog : BaseDialog<DialogAddGroupBinding>(), View.OnClickListener {

  private lateinit var loadDialog: LoadingDialog
  private var icon = PwIconStandard(48)
  private var csIcon: PwIconCustom? = null
  private var group: PwGroup? = null
  private lateinit var module: CreateEntryModule
  var parentGroup: PwGroup = BaseApp.KDB!!.pm.rootGroup

  companion object {
    fun generate(body: CreateGroupDialog.() -> CreateGroupDialog): CreateGroupDialog {
      return with(CreateGroupDialog()) { body() }
    }
  }

  fun build(): CreateGroupDialog {
    return this
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_group
  }

  override fun initData() {
    super.initData()
    module = ViewModelProvider(this).get(CreateEntryModule::class.java)
    binding.groupNameLayout.setEndIconOnClickListener {
      showIconDialog()
    }
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
  }

  private fun showIconDialog() {
    val iconDialog = IconBottomSheetDialog()
    iconDialog.setCallback(object : IconItemCallback {
      override fun onDefaultIcon(defIcon: PwIconStandard) {
        icon = defIcon
        binding.groupNameLayout.endIconDrawable =
          resources.getDrawable(IconUtil.getIconById(icon.iconId), requireContext().theme)
        csIcon = PwIconCustom.ZERO
      }

      override fun onCustomIcon(customIcon: PwIconCustom) {
        csIcon = customIcon
        binding.groupNameLayout.endIconDrawable =
          IconUtil.convertCustomIcon2Drawable(requireContext(), csIcon!!)
      }

    })
    iconDialog.show(childFragmentManager, IconBottomSheetDialog::class.java.simpleName)
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.enter -> {
        val title = binding.groupName.text.toString()
            .trim()
        if (title.isEmpty()) {
          HitUtil.toaskShort(getString(R.string.error_group_name_null))
          return
        }
        if (title.length > 16) {
          HitUtil.toaskShort(requireContext().getString(R.string.title_too_long))
          return
        }
        loadDialog = LoadingDialog(context)
        loadDialog.show()
        createGroup()
      }
      R.id.cancel -> {
        dismiss()
      }
    }
  }

  /**
   * 创建群组
   */
  private fun createGroup() {
    module.createGroup(
        binding.groupName.text.toString()
            .trim(),
        parentGroup,
        icon,
        csIcon
    )
        .observe(this, Observer { newGroup ->
          this.group = newGroup
          loadDialog.dismiss()
          if (newGroup == null) {
            HitUtil.toaskShort(getString(R.string.create_group_fail))
            return@Observer
          }
          HitUtil.toaskShort(getString(R.string.create_group_success))
          EventBus.getDefault()
              .post(CreateOrUpdateGroupEvent(newGroup))
          dismiss()
        })
  }
}