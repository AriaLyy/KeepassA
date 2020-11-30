/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.widget.editor

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.text.StringBuilder

/**
 * @Author laoyuyu
 * @Description editor
 * @Date 2020/11/30
 **/
class EditorView(
  context: Context,
  attributeSet: AttributeSet
) : AppCompatEditText(context, attributeSet) {
  private val TAG = javaClass.simpleName
  private var scope = MainScope()
  private val INTERVAL = 1000L
  private var isAdding = false
  private var lastOperateTime = System.currentTimeMillis()
  private var sb = StringBuilder()
  private val operateManager = OperateManager()
  private var isOperating = false

  override fun onFinishInflate() {
    super.onFinishInflate()
    // 关闭拼写检查，防止onTextChanged错乱问题
    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    isSingleLine = false
    addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      /**
       * s: 改变后的字符串
       * start: 有变动的字符串的序号
       * before: 被改变的字符串长度，如果是新增则为 0。
       * count: 添加的字符串长度，如果是删除则为 0。
       */
      override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
      ) {
//        Log.d(TAG, "onTextChanged: s = " + s + ", start = " + start +
//            ", before = " + before + ", count = " + count)
        if (s.isNullOrEmpty() || isOperating) return
        // add new str
        if (before == 0) {
          val newStr = s.subSequence(start, start + count)
          if (System.currentTimeMillis() - lastOperateTime > INTERVAL) {
            addNewStr(newStr)
            return
          }
          sb.append(newStr)
          return
        }
        /*
          delete new str
         */
        // before del str need add str form cache, and clear cache
        addNewStr(null)
        val delStr = s.subSequence(start - before, start)
        operateManager.delete(delStr)
      }

      override fun afterTextChanged(s: Editable?) {

      }

    })
  }

  override fun onTextChanged(
    text: CharSequence?,
    start: Int,
    lengthBefore: Int,
    lengthAfter: Int
  ) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    isOperating = false
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!scope.isActive) {
      scope = MainScope()
    }
    scope.launch(Dispatchers.IO) {
      repeat(Int.MAX_VALUE) {
        delay(INTERVAL * 5)
        if (!isAdding) {
          addNewStr(null)
        }
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    scope.cancel()
  }

  /**
   * add new str to container
   */
  private fun addNewStr(s: CharSequence?) {
    isAdding = true
    s?.let { sb.append(it) }
    lastOperateTime = System.currentTimeMillis()
    if (sb.isEmpty()) {
      isAdding = false
      return
    }
    val str = sb.toString()
    Log.d(TAG, "addNewStr = $str")
    operateManager.add(str)
    sb.clear()
    isAdding = false
  }

  fun addOperateStr(s: CharSequence) {
    operateManager.add(s)
  }

  fun undo() {
    isOperating = true
    val newStr = operateManager.undo()
    setText(newStr)
    setSelection(newStr.length)
  }

  fun redo() {
    isOperating = true
    val newStr = operateManager.redo()
    setText(newStr)
    setSelection(newStr.length)
  }

  fun clear() {
    isOperating = true
    val newStr = operateManager.clear()
    setText(newStr)
    setSelection(newStr.length)
  }

}