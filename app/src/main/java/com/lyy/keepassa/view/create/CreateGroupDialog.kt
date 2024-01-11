/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogAddGroupBinding
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.create.entry.CreateEntryModule
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback

/**
 * 创建或编辑群组dialog
 */
@Route(path = "/dialog/createGroup")
class CreateGroupDialog : BaseDialog<DialogAddGroupBinding>(), View.OnClickListener {

  private var icon = PwIconStandard(48)
  private var csIcon: PwIconCustom? = null
  private lateinit var module: CreateEntryModule

  @Autowired(name = "parentGroup")
  @JvmField
  var parentGroup: PwGroupV4 = BaseApp.KDB!!.pm.rootGroup as PwGroupV4

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_group
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
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
      binding.groupName.text.toString(),
      parentGroup,
      icon,
      csIcon
    ) {
      HitUtil.toaskShort(
        "${BaseApp.APP.getString(R.string.create_group)}${
          BaseApp.APP.getString(
            R.string.success
          )
        }"
      )
      dismiss()
    }
  }
}