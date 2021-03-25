/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.icon

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.arialyy.frame.util.adapter.RvItemClickSupport
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwIconCustom
import com.keepassdroid.database.PwIconStandard
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseBottomSheetDialogFragment
import com.lyy.keepassa.databinding.DialogEntryIconBinding
import com.lyy.keepassa.entity.SimpleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/3/24
 **/
class IconBottomSheetDialog : BaseBottomSheetDialogFragment<DialogEntryIconBinding>() {

  companion object {
    val ICON_TYPE_DEFAULT = 0
    val ICON_TYPE_CUSTOM = 1
  }

  private lateinit var behavior: BottomSheetBehavior<RelativeLayout>
  private lateinit var module: IconModule
  private var scope = MainScope()
  private var curType = ICON_TYPE_DEFAULT
  private var callback: IconItemCallback? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_entry_icon
  }

  override fun init(savedInstanceState: Bundle?) {
    super.init(savedInstanceState)
    behavior = BottomSheetBehavior.from(binding.llContent)
    module = ViewModelProvider(requireActivity()).get(IconModule::class.java)
    if (!BaseApp.isV4 || (BaseApp.KDB.pm as PwDatabaseV4).customIcons.isNullOrEmpty()) {
      binding.rg.visibility = View.GONE
    }
    behavior.state = STATE_EXPANDED
    initWidget()
  }

  fun setCallback(callback: IconItemCallback) {
    this.callback = callback
  }

  private fun initWidget() {
    val data = arrayListOf<SimpleItemEntity>()
    val adapter = IconAdapter(requireContext(), data)
    binding.rvList.adapter = adapter
    binding.rvList.layoutManager = GridLayoutManager(requireContext(), 6)
    binding.rvList.hasFixedSize()
    val observer = Observer<List<SimpleItemEntity>> {
      data.clear()
      data.addAll(it)
      adapter.notifyDataSetChanged()
      hideLoadingAnim()
    }

    showLoadingAnim()
    module.getDefaultIconList()
        .observe(this, observer)

    binding.rg.setOnCheckedChangeListener { _, checkedId ->
      showLoadingAnim()
      if (checkedId == R.id.mrbDefault) {
        curType = ICON_TYPE_DEFAULT
        module.getDefaultIconList()
            .observe(this, observer)
        return@setOnCheckedChangeListener
      }
      curType = ICON_TYPE_CUSTOM
      module.getCustomIconList()
          .observe(this, observer)
    }

    RvItemClickSupport.addTo(binding.rvList)
        .setOnItemClickListener { _, position, _ ->
          val item = data[position]
          if (curType == ICON_TYPE_DEFAULT) {
            callback?.onDefaultIcon(PwIconStandard(item.icon))
            dismiss()
            return@setOnItemClickListener
          }
          callback?.onCustomIcon(item.obj as PwIconCustom)
          dismiss()
        }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }

  private fun showLoadingAnim() {
    val animationView = getLadingView()
    animationView.visibility = View.VISIBLE
    animationView.playAnimation()
  }

  private fun hideLoadingAnim() {
    scope.launch {
      withContext(Dispatchers.IO) {
        delay(1000)
      }
      val animationView = getLadingView()
      animationView.cancelAnimation()
      animationView.visibility = View.GONE
    }
  }

  private fun getLadingView(): LottieAnimationView {
    if (!binding.vsLoading.isInflated) {
      binding.vsLoading.viewStub?.inflate()
    }
    val anim = binding.vsLoading.root as LottieAnimationView
    anim.repeatCount = 10
    return anim
  }

}

interface IconItemCallback {
  fun onDefaultIcon(defIcon: PwIconStandard)
  fun onCustomIcon(customIcon: PwIconCustom)
}