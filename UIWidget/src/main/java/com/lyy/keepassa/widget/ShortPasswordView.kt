/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget

import android.content.Context
import android.content.res.TypedArray
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.uiwidget.R

/**
 * 短密码
 */
class ShortPasswordView(
  context: Context,
  attributeSet: AttributeSet
) : RelativeLayout(context, attributeSet) {

  private var TAG = javaClass.simpleName
  private val llBar: LinearLayout
  private val passEt: MyEditText
  private var passLen = 6
  private val tvs = ArrayList<ShortPassItem>()
  private var inputContent = ""

  private var inputCompleteListener: InputCompleteListener? = null

  interface InputCompleteListener {
    fun inputComplete(text: String)
    fun invalidContent()
  }

  init {
    LayoutInflater.from(context).inflate(R.layout.layout_short_password, this, true)
    llBar = findViewById(R.id.llTvBar)
    passEt = findViewById(R.id.etPass)
    val ta: TypedArray =
      context.obtainStyledAttributes(attributeSet, R.styleable.ShortPasswordView)
    passLen = ta.getInteger(R.styleable.ShortPasswordView_passLen, 3)
    ta.recycle()
    if (passLen <= 0 || passLen > 6) {
      Log.e(TAG, "密码长度错误，不能小于0，并且不能大于6")
    } else {
      addItem(passLen)
    }
    setEditTextListener()
    passEt.isEnabled = false
  }

  fun setInputCompleteListener(inputCompleteListener: InputCompleteListener?) {
    this.inputCompleteListener = inputCompleteListener
  }

  private fun setEditTextListener() {
    passEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
      }

      override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
      ) {
        if (tvs.isEmpty()) {
          return
        }
        inputContent = passEt.text.toString()
        if (inputCompleteListener != null) {
          if (TextUtils.isEmpty(inputContent)) {
            for (i in 0 until passLen) {
              tvs[i].setText("")
            }
            return
          }
          if (inputContent.length >= passLen) {
            inputCompleteListener!!.inputComplete(inputContent)
          } else {
            inputCompleteListener!!.invalidContent()
          }
        }

        for (i in 0 until passLen) {
          if (i < inputContent.length) {
            tvs[i].setText(inputContent[i].toString())
          } else {
            tvs[i].setText("")
          }
        }
      }
    })
  }

  /**
   * 清空密码
   */
  fun clean() {
    inputContent = ""
    passEt.setText("")
    if (tvs.isEmpty()) {
      return
    }
    for (i in 0 until passLen) {
      tvs[i].setText("")
    }
  }

  override fun onKeyDown(
    keyCode: Int,
    event: KeyEvent?
  ): Boolean {
    // 防止电脑键盘直接输入时，密码长度异常的问题
    passEt.requestFocus()
    return super.onKeyDown(keyCode, event)
  }

  /**
   * 设置密码长度，最大6
   */
  fun setPassLen(len: Int) {
    clean()
    tvs.clear()
    if (len > 6) {
      Log.e(TAG, "最大长度为6")
      return
    }
    passLen = len
    addItem(passLen)
    passEt.filters = arrayOf<InputFilter>(LengthFilter(passLen))
    passEt.isEnabled = true
    passEt.requestFocus()
  }

  /**
   * 动态增加item
   */
  private fun addItem(passLen: Int) {
    var layoutW = 0
    val itemW = context.resources.getDimension(R.dimen.short_item_w).toInt()
    val itemH = context.resources.getDimension(R.dimen.short_item_h).toInt()
    val spaceW = context.resources.getDimension(R.dimen.short_item_space_w).toInt()
    for (i in 0 until passLen) {
      val item = ShortPassItem(context)
      var tempW = itemW

      if (i == passLen - 1) {
        item.showSpace(false)
      } else {
        tempW += spaceW
      }
      layoutW += tempW
      llBar.addView(item, LayoutParams(tempW, itemH))
      tvs.add(item)
    }
    val lp = layoutParams
    lp.width = layoutW
    lp.height = 49.toPx()
  }

  /**
   * 短密码item
   */
  class ShortPassItem(context: Context) : RelativeLayout(context) {

    private val tv: TextView
    private val space: View

    init {
      LayoutInflater.from(context).inflate(R.layout.item_short_password, this, true)
      tv = findViewById(R.id.tvItem)
      space = findViewById(R.id.vSpace)
    }

    fun setText(
      text: CharSequence,
      showSpace: Boolean = true
    ) {
      tv.text = text
      space.visibility = if (showSpace) View.VISIBLE else View.GONE
    }

    fun showSpace(show: Boolean) {
      space.visibility = if (show) View.VISIBLE else View.GONE
    }

  }

}