/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RadioButton
import androidx.lifecycle.ViewModelProvider
import com.arialyy.frame.router.Routerfit
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentCreateDbSecondBinding
import com.lyy.keepassa.event.KeyPathEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.EventBusHelper
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.widget.BubbleTextView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * 设置密码、密钥等信息
 */
class CreateDbSecondFragment : BaseFragment<FragmentCreateDbSecondBinding>(),
  BubbleTextView.OnIconClickListener {

  private var keyPassLayoutH: Int = 0
  private var isShowPass = false
  private lateinit var module: CreateDbModule

  override fun setLayoutId(): Int {
    return R.layout.fragment_create_db_second
  }

  @SuppressLint("RestrictedApi")
  override fun initData() {
    EventBusHelper.reg(this)
    module = ViewModelProvider(requireActivity()).get(CreateDbModule::class.java)
    binding.dbName.setText(module.dbName)
    val leftDrawable = resources.getDrawable(module.storageType.icon, requireContext().theme)
    val iconSize = resources.getDimension(R.dimen.icon_size)
    leftDrawable.setBounds(0, 0, iconSize.toInt(), iconSize.toInt())
    binding.dbHint.setCompoundDrawables(leftDrawable, null, null, null)
    binding.encryptGroup.setOnCheckedChangeListener { group, checkedId ->
      val rb = group.findViewById<RadioButton>(checkedId)
      if (rb.tag == "1") {
//          binding.passKeyLayout.visibility = View.GONE
        hintPassLayout()
      } else {
//          binding.passKeyLayout.visibility = View.VISIBLE
        showPassLayout()
      }
    }
    binding.encryptType.setOnIconClickListener(this)
    binding.passKey.setOnIconClickListener(this)
    (binding.encryptGroup.getChildAt(0) as RadioButton).isChecked = true
    binding.passKeyLayout.post {
      keyPassLayoutH = binding.passKeyLayout.height
    }
    binding.chooseBt.setOnClickListener {
      val dialog = CreatePassKeyDialog()
      dialog.show(childFragmentManager, "passKeyDialog")
    }
    KeepassAUtil.instance.toggleKeyBord(requireContext())
    binding.password.requestFocus()
    binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view_off)
//    binding.password.imeOptions = EditorInfo.IME_ACTION_NEXT

    binding.passwordLayout.setEndIconOnClickListener {
      isShowPass = !isShowPass
      if (isShowPass) {
        binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view)
        binding.enterPasswordLayout.visibility = View.GONE
        binding.password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        // 重新修改确认按钮
//        binding.password.imeOptions = EditorInfo.IME_ACTION_NEXT
      } else {
        binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view_off)
        binding.enterPasswordLayout.visibility = View.VISIBLE
        binding.password.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT)

        // 重新修改确认按钮
//        binding.password.imeOptions = EditorInfo.IME_ACTION_DONE
      }
      // 将光标移动到最后
      binding.password.setSelection(binding.password.text!!.length)
      binding.password.requestFocus()
    }
  }

  private fun showPassLayout() {
    val h = resources.getDimension(R.dimen.create_pass_key_h)
      .toInt()
    binding.passKeyLayoutWrap.visibility = View.VISIBLE
    binding.passKeyLayout.layoutParams.height = 0
    binding.passKeyLayout.visibility = View.VISIBLE
    val anim = ValueAnimator.ofInt(0, h)
    anim.addUpdateListener { animation ->
      binding.passKeyLayout.layoutParams.height = animation.animatedValue as Int
      binding.passKeyLayout.requestLayout()
    }
    anim.interpolator = LinearInterpolator()
    anim.duration = 400
    anim.start()
  }

  private fun hintPassLayout() {
    val h = resources.getDimension(R.dimen.create_pass_key_h)
      .toInt()
    binding.passKeyLayoutWrap.visibility = View.VISIBLE
    binding.passKeyLayout.layoutParams.height = 0
    binding.passKeyLayout.visibility = View.VISIBLE
    module.keyUri = null
    val anim = ValueAnimator.ofInt(h, 0)
    anim.addUpdateListener { animation ->
      binding.passKeyLayout.layoutParams.height = animation.animatedValue as Int
      binding.passKeyLayout.requestLayout()
    }
    anim.interpolator = LinearInterpolator()
    anim.duration = 400
    anim.start()
    anim.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        binding.passKeyLayout.visibility = View.GONE
        binding.passKeyLayoutWrap.visibility = View.GONE
      }
    })
  }

  /**
   * 获取密码，如果两次密码不一致，返回null
   */
  fun getPass(): String? {
    val pass = binding.password.text.toString()
      .trim()
    val enterPass = binding.enterPassword.text.toString()
      .trim()
    if (TextUtils.isEmpty(pass)) {
      HitUtil.toaskShort(getString(R.string.error_pass_null))
      return null
    }
    // 如果没有显示密码，需要判断两次输入的密码是否一致
    if (!isShowPass) {
      if (TextUtils.isEmpty(enterPass)) {
        HitUtil.toaskShort(getString(R.string.error_enter_pass_null))
        binding.enterPassword.requestFocus()
        KeepassAUtil.instance.toggleKeyBord(requireContext())
        return null
      }
      if (!pass.equals(enterPass, false)) {
        HitUtil.toaskShort(getString(R.string.error_pass_unfit))
        return null
      }
    }

    module.dbPass = pass

    return module.dbPass
  }

  /**
   * 获取key的路径
   */
  @Subscribe(threadMode = MAIN)
  fun onKeyEvent(event: KeyPathEvent) {
    module.keyUri = event.keyUri
    module.keyName = event.keyName
    binding.passKeyName.setText(module.keyName)
  }

  fun getShareElement(): View {
    return binding.dbName
  }

  override fun onClick(
    view: BubbleTextView,
    index: Int
  ) {
    var msg = ""
    when (view.id) {
      R.id.encrypt_type -> {
        msg = getString(R.string.help_pass_type)
      }
      R.id.pass_key -> {
        msg = getString(R.string.help_pass_key)
      }
    }
    Routerfit.create(DialogRouter::class.java).showMsgDialog(msgContent = msg, showCancelBt = false)
  }

  override fun onDestroy() {
    super.onDestroy()
    EventBusHelper.unReg(this)
  }
}