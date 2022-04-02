/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.PwGroupV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogAddGroupBinding
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.icon.IconBottomSheetDialog
import com.lyy.keepassa.view.icon.IconItemCallback

/**
 * 编辑群组
 */
@Route(path = "/dialog/modifyGroup")
class ModifyGroupDialog : BaseDialog<DialogAddGroupBinding>(), View.OnClickListener {

  private var icon: PwIconStandard = PwIconStandard(1)
  private var csIcon: PwIconCustom? = null

  @Autowired(name = "pwGroup")
  @JvmField
  var pwGroup: PwGroupV4? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_group
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    if (pwGroup == null) {
      dismiss()
      return
    }
    binding.groupNameLayout.setEndIconOnClickListener {
      showIconDialog()
    }
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
    binding.title.text = getString(R.string.modify_group)

    pwGroup?.let {
      binding.groupName.setText(it.name)
      binding.groupNameLayout.endIconDrawable =
        IconUtil.getGroupIconDrawable(requireContext(), it, true)
      icon = it.icon
      csIcon = it.customIcon
    }
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
        val newTitle = binding.groupName.text.toString()
          .trim()
        if (newTitle.isEmpty()) {
          HitUtil.toaskShort(getString(R.string.error_group_name_null))
          return
        }
        KpaUtil.kdbHandlerService.modifyGroup(newTitle, icon, csIcon, pwGroup!!) {
          dismiss()
        }
      }
      R.id.cancel -> {
        dismiss()
      }
    }
  }
}