/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.databinding.DialogReviewBinding
import com.lyy.keepassa.entity.ReviewBean
import com.lyy.keepassa.router.ServiceRouter
import com.lyy.keepassa.util.CommonKVStorage

/**
 * @Author laoyuyu
 * @Description
 * @Date 2024/5/8
 **/
class ReviewDialog : BaseDialog<DialogReviewBinding>() {
  override fun setLayoutId(): Int {
    return R.layout.dialog_review
  }

  override fun initData() {
    super.initData()
    binding.clicker = object : DialogBtnClicker {
      override fun onEnter(v: View) {
        super.onEnter(v)
        Routerfit.create(ServiceRouter::class.java).getPlayService().review(requireActivity())
        dismiss()
      }

      override fun onCancel(v: View) {
        super.onCancel(v)
        dismiss()
      }
    }

    binding.cbNotHint.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        val reviewBean =
          CommonKVStorage.get(Constance.KEY_REVIEW, ReviewBean::class.java, null) ?: ReviewBean(
            false
          )
        reviewBean.isAlreadyReview = true
        CommonKVStorage.put(Constance.KEY_REVIEW, reviewBean)
      }
    }
  }
}