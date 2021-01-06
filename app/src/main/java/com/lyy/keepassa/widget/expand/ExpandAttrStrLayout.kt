/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.expand

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.children
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.OtpUtil
import com.lyy.keepassa.widget.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 项目详情页，展开自定义字段
 */
class ExpandAttrStrLayout(
  context: Context,
  attr: AttributeSet
) : RelativeLayout(context, attr), View.OnClickListener {
  private val TAG = javaClass.simpleName
  private var attrIsExpand = false // 属性字段展开状态
  private val animDuring = 400L
  private var showAttrAnim: ObjectAnimator? = null
  private var hintAttrAnim: ObjectAnimator? = null
  private var attrTitleX = 0F
  private var strData = LinkedHashMap<String, ProtectedString>()
  private var fileData = HashMap<String, ProtectedBinary>()
  private val itemAnimDuring = 500L

  private val attrImg: AppCompatImageView
  private val attrTt: TextView
  private val attrArrow: AppCompatImageView
  private var listener: OnExpandListener? = null
  private val itemLayout: LinearLayout
  private var strViewListener: OnAttrViewClickListener? = null

  // otp 密码字段，将会自动更新密码
  private var otpPassItem: AttrStrItemView? = null

  private var job: Job? = null
  var entryV4: PwEntryV4? = null
  private val scope = MainScope()

  /**
   * 展开事件
   */
  interface OnExpandListener {
    fun onExpand(isExpand: Boolean)
  }

  /**
   * 高级属性的view点击事件
   */
  interface OnAttrViewClickListener {
    fun onClickListener(
      v: View,
      position: Int
    )
  }

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_expand_title, this, true)
    attrImg = findViewById(R.id.attr_img)
    attrTt = findViewById(R.id.attr_tt)
    attrArrow = findViewById(R.id.attr_arrow)
    itemLayout = findViewById(R.id.attrs)
    val ta = context.obtainStyledAttributes(attr, R.styleable.ExpandTextView)
    val tStr = ta.getString(R.styleable.ExpandTextView_expand_tt_title)
    val iDrawable = ta.getDrawable(R.styleable.ExpandTextView_expand_tt_icon)
    ta.recycle()
    attrTt.text = tStr
    if (iDrawable != null) {
      attrImg.setImageDrawable(iDrawable)
    }
    // 需在重新设置一次背景，inflate进去并不是root
//    rootView.setBackgroundResource(R.drawable.ripple_white_selector)

    findViewById<View>(R.id.head).setOnClickListener {
      toggle()
    }

    // 设置visibility 和 gone时播放的动画
//    initAnim()
  }

  @SuppressLint("ObjectAnimatorBinding")
  private fun initAnim() {
    val mTransitioner = LayoutTransition()

//    val pvhLeft = PropertyValuesHolder.ofInt("left", 0, 0)
//    val pvhTop = PropertyValuesHolder.ofInt("top", 0, 0)
//    val pvhRight = PropertyValuesHolder.ofInt("right", 0, 0)
//    val pvhBottom = PropertyValuesHolder.ofInt("bottom", 0, 0)
//
//    val animator = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 1f)
//    val changeIn = ObjectAnimator.ofPropertyValuesHolder(
//            this, pvhLeft, pvhBottom, animator
//        )
//        .setDuration(mTransitioner.getDuration(LayoutTransition.CHANGE_APPEARING))

    mTransitioner.setStartDelay(LayoutTransition.APPEARING, 1000)
    mTransitioner.setStartDelay(LayoutTransition.CHANGE_APPEARING, 1000)
//    mTransitioner.setAnimator(LayoutTransition.CHANGE_APPEARING, changeIn)

//    val pvhRotation = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 1f)
//    val changeOut = ObjectAnimator.ofPropertyValuesHolder(
//            this, pvhLeft, pvhBottom, pvhRotation
//        )
//        .setDuration(mTransitioner.getDuration(LayoutTransition.CHANGE_DISAPPEARING))

//    mTransitioner.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, changeOut)

    mTransitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 1000)
    mTransitioner.disableTransitionType(LayoutTransition.APPEARING)
    mTransitioner.disableTransitionType(LayoutTransition.DISAPPEARING)
    mTransitioner.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
    mTransitioner.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
    layoutTransition = mTransitioner

  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    job?.cancel()
    scope.cancel()
  }

  override fun removeAllViews() {
    itemLayout.removeAllViews()
    strData.clear()
    fileData.clear()
  }

  private fun toggle() {
    if ( KeepassAUtil.instance.isFastClick()) {
      return
    }
    if (attrIsExpand) {
      hintAttr()
    } else {
      showAttr()
    }
  }

  /**
   * 设置高级属性的view点击事件
   */
  fun setOnAttrViewClickListener(listener: OnAttrViewClickListener) {
    this.strViewListener = listener
  }

  /**
   * 设置展开事件监听
   */
  fun setOnExpandListener(listener: OnExpandListener) {
    this.listener = listener
  }

  /**
   * 隐藏属性字段
   */
  private fun hintAttr() {
    // 处理icon 和标题
    attrImg.visibility = View.VISIBLE
    ObjectAnimator.ofFloat(attrTt, "translationX", attrTt.translationX, attrTitleX)
        .setDuration(200)
        .start()

    // 处理箭头动画
    if (hintAttrAnim != null && hintAttrAnim!!.isRunning) {
      hintAttrAnim!!.cancel()
    }
    if (showAttrAnim == null) {
      showAttrAnim = ObjectAnimator.ofFloat(attrArrow, "rotation", -90f, 0f)
      showAttrAnim!!.duration = animDuring
    }
    showAttrAnim!!.start()
    attrIsExpand = false
    if (listener != null) {
      listener!!.onExpand(false)
    }
    for (v in itemLayout.children) {
      v.visibility = View.GONE
    }
    itemLayout.visibility = View.GONE
  }

  /**
   *  显示属性字段
   */
  private fun showAttr() {
    // 处理icon 和标题
    val iconX = attrImg.translationX - 16.toPx()
    attrTitleX = attrTt.translationX
    attrImg.visibility = View.GONE
    ObjectAnimator.ofFloat(attrTt, "translationX", attrTitleX, iconX)
        .setDuration(200)
        .start()

    // 处理箭头动画
    if (showAttrAnim != null && showAttrAnim!!.isRunning) {
      showAttrAnim!!.cancel()
    }
    if (hintAttrAnim == null) {
      hintAttrAnim = ObjectAnimator.ofFloat(attrArrow, "rotation", 0f, -90f)
      hintAttrAnim!!.duration = animDuring
    }
    hintAttrAnim!!.start()
    attrIsExpand = true
    if (listener != null) {
      listener!!.onExpand(true)
    }

    for (v in itemLayout.children) {
      v.visibility = View.VISIBLE
    }
    itemLayout.visibility = View.VISIBLE
  }

  /**
   * 增加自定义属性
   */
  fun setFileValue(map: Map<String, ProtectedBinary>) {
    fileData.putAll(map)
    if (fileData.isNotEmpty()) {
      var i = 0
      for (d in fileData) {
        val child = AttrFileItemView(context, d.key, d.value)
        child.id = i
        child.setOnClickListener(this)
        child.isClickable = true
        itemLayout.addView(child, i)
        i++
      }
    }
    itemLayout.invalidate()
  }

  /**
   * 增加一个自定义属性
   */
  fun addFileValue(
    title: String,
    value: ProtectedBinary
  ) {
    fileData[title] = value
    val child = AttrFileItemView(
        context, title, value
    )
    child.id = itemLayout.childCount
    child.setOnClickListener(this)
    itemLayout.addView(child, child.id)
    itemLayout.invalidate()
  }

  /**
   * 增加自定义属性
   */
  fun setAttrValue(map: Map<String, ProtectedString>) {
    strData.putAll(map)
    if (strData.isNotEmpty()) {
      var i = 0
      for (d in strData) {
        val child = AttrStrItemView(context, d.key, d.value)
        child.id = i
        child.setOnClickListener(this)
        child.isClickable = true
        itemLayout.addView(child, i)
        if (d.value.isOtpPass) {
          otpPassItem = child
        }
        i++
      }
    }
    itemLayout.invalidate()
    startAutoGetOtp()
  }

  /**
   * 定时自动获取otp密码
   */
  private fun startAutoGetOtp() {
    if (otpPassItem == null || otpPassItem?.pb == null || entryV4 == null) {
      KLog.e(TAG, "无法自动获取otp密码")
      return
    }
    val p = OtpUtil.getOtpPass(entryV4!!)
    if (p.second.isNullOrEmpty()){
      KLog.e(TAG, "无法自动获取otp密码")
      return
    }

    otpPassItem!!.pb.visibility = View.VISIBLE
    otpPassItem!!.pb.setCountdown(true)

    scope.launch(Dispatchers.Main) {

      Log.d(TAG, p.toString())
      val time = p.first
      otpPassItem!!.pb.max = time
      otpPassItem!!.valueTx.text = p.second
      for (i in time downTo 1) {
        otpPassItem!!.pb.progress = i
        withContext(Dispatchers.IO) {
          delay(1000)
        }
      }
      startAutoGetOtp()
    }
  }

  /**
   * 增加一个自定义属性
   */
  fun addStrValue(
    title: String,
    value: ProtectedString
  ) {
    strData[title] = value
    val child = AttrStrItemView(context, title, value)
    child.id = itemLayout.childCount + 1
    child.setOnClickListener(this)
    itemLayout.addView(child, childCount + 1)
    itemLayout.invalidate()
  }

  /**
   * 删除自定义属性字段
   */
  fun removeStrValue(id: Int) {
    if (id < 0 || id > childCount) {
      Log.e(TAG, "id【$id】错误")
      return
    }
    val child = getChildAt(id) as AttrStrItemView
    strData.remove(child.titleStr)
    itemLayout.removeViewAt(id)
    itemLayout.invalidate()
  }

  override fun onClick(v: View?) {
    if (strViewListener != null) {
      strViewListener!!.onClickListener(v!!, v.id)
    }
  }

}